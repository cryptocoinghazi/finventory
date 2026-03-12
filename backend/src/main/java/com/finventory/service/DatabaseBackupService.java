package com.finventory.service;

import com.finventory.dto.DatabaseBackupDto;
import com.finventory.model.DatabaseBackup;
import com.finventory.model.DatabaseBackupStatus;
import com.finventory.repository.DatabaseBackupRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DatabaseBackupService {

    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);
    private static final int DEFAULT_POSTGRES_PORT = 5432;
    private static final int MAX_PGDUMP_OUTPUT_LINES = 200;
    private static final int MAX_ERROR_LENGTH = 4000;

    private final DatabaseBackupRepository databaseBackupRepository;
    private final AuditLogService auditLogService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Value("${application.backup.dir:backups}")
    private String backupDir;

    @Value("${application.backup.pg-dump-path:}")
    private String pgDumpPath;

    @Value("${application.backup.retention-count:20}")
    private int retentionCount;

    @Transactional(readOnly = true)
    public List<DatabaseBackupDto> listBackups() {
        return databaseBackupRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public DatabaseBackupDto createBackup(String requestedBy) {
        OffsetDateTime now = OffsetDateTime.now();
        DatabaseBackup backup =
                DatabaseBackup.builder()
                        .status(DatabaseBackupStatus.RUNNING)
                        .createdAt(now)
                        .startedAt(now)
                        .requestedBy(requestedBy)
                        .build();
        backup = databaseBackupRepository.save(backup);

        try {
            Path dir = resolveBackupDir();
            Files.createDirectories(dir);

            String fileName = "finventory-backup-" + now.format(FILE_TS) + ".sql";
            Path outputFile = dir.resolve(fileName).normalize();

            if (isPostgresDatasource()) {
                runPgDump(outputFile);
            } else {
                String payload =
                        "CREATE TABLE __finventory_backup_probe(id INT);\n"
                                + "DROP TABLE __finventory_backup_probe;\n";
                Files.writeString(outputFile, payload, StandardCharsets.UTF_8);
            }

            long size = Files.size(outputFile);
            backup.setStatus(DatabaseBackupStatus.SUCCESS);
            backup.setFinishedAt(OffsetDateTime.now());
            backup.setFileName(fileName);
            backup.setFilePath(fileName);
            backup.setFileSize(size);
            backup.setErrorMessage(null);
            DatabaseBackup saved = databaseBackupRepository.save(backup);

            auditLogService.log(
                    "DB_BACKUP_SUCCESS",
                    "DATABASE_BACKUP",
                    saved.getId(),
                    "fileName=" + fileName + ", size=" + size);
            applyRetention(dir);
            return mapToDto(saved);
        } catch (Exception e) {
            backup.setStatus(DatabaseBackupStatus.FAILED);
            backup.setFinishedAt(OffsetDateTime.now());
            backup.setErrorMessage(truncate(e.getMessage()));
            DatabaseBackup saved = databaseBackupRepository.save(backup);
            auditLogService.log(
                    "DB_BACKUP_FAILED",
                    "DATABASE_BACKUP",
                    saved.getId(),
                    "error=" + truncate(e.getMessage()));
            return mapToDto(saved);
        }
    }

    @Transactional(readOnly = true)
    public DatabaseBackup getBackup(UUID id) {
        return databaseBackupRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Backup not found"));
    }

    public Path resolveBackupFile(DatabaseBackup backup) {
        if (backup.getFilePath() == null || backup.getFilePath().isBlank()) {
            throw new IllegalArgumentException("Backup file not available");
        }
        Path dir = resolveBackupDir();
        Path file = dir.resolve(backup.getFilePath()).normalize();
        if (!file.startsWith(dir)) {
            throw new IllegalArgumentException("Invalid backup path");
        }
        return file;
    }

    private Path resolveBackupDir() {
        Path raw = Paths.get(backupDir);
        return raw.toAbsolutePath().normalize();
    }

    private boolean isPostgresDatasource() {
        if (datasourceUrl == null) {
            return false;
        }
        return datasourceUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:postgresql:");
    }

    private void runPgDump(Path outputFile) throws Exception {
        PostgresInfo info = parsePostgresUrl(datasourceUrl);
        Path resolvedPgDump = resolvePgDumpPath();

        List<String> command = new ArrayList<>();
        command.add(resolvedPgDump.toString());
        command.add("-h");
        command.add(info.host());
        command.add("-p");
        command.add(Integer.toString(info.port()));
        command.add("-U");
        command.add(datasourceUsername);
        command.add("-F");
        command.add("p");
        command.add("-f");
        command.add(outputFile.toString());
        command.add(info.database());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        if (datasourcePassword != null && !datasourcePassword.isBlank()) {
            pb.environment().put("PGPASSWORD", datasourcePassword);
        }

        Process p = pb.start();
        String out;
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            out = reader.lines().limit(MAX_PGDUMP_OUTPUT_LINES).reduce("", (a, b) -> a + b + "\n");
        }
        int code = p.waitFor();
        if (code != 0) {
            throw new IllegalStateException(
                    "pg_dump failed (exit=" + code + "): " + truncate(out.trim()));
        }
        if (!Files.exists(outputFile) || Files.size(outputFile) == 0) {
            throw new IllegalStateException("pg_dump did not produce output");
        }
    }

    private Path resolvePgDumpPath() {
        if (pgDumpPath != null && !pgDumpPath.isBlank()) {
            return Paths.get(pgDumpPath).toAbsolutePath().normalize();
        }

        List<Path> candidates =
                List.of(
                        Paths.get("tools", "pgsql", "bin", "pg_dump.exe"),
                        Paths.get("tools", "pgsql", "pgAdmin 4", "runtime", "pg_dump.exe"),
                        Paths.get("backend", "tools", "pgsql", "bin", "pg_dump.exe"),
                        Paths.get("backend", "tools", "pgsql", "pgAdmin 4", "runtime", "pg_dump.exe"),
                        Paths.get("..", "tools", "pgsql", "bin", "pg_dump.exe"),
                        Paths.get("..", "tools", "pgsql", "pgAdmin 4", "runtime", "pg_dump.exe"));
        for (Path c : candidates) {
            Path abs = c.toAbsolutePath().normalize();
            if (Files.exists(abs)) {
                return abs;
            }
        }
        return Paths.get("pg_dump");
    }

    private PostgresInfo parsePostgresUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException("Datasource URL missing");
        }
        String prefix = "jdbc:postgresql://";
        String trimmed = url.trim();
        if (!trimmed.toLowerCase(Locale.ROOT).startsWith(prefix)) {
            throw new IllegalArgumentException("Unsupported datasource: " + url);
        }

        String rest = trimmed.substring(prefix.length());
        int slash = rest.indexOf('/');
        if (slash < 0) {
            throw new IllegalArgumentException("Invalid postgres URL: " + url);
        }
        String hostPort = rest.substring(0, slash);
        String dbPart = rest.substring(slash + 1);
        int q = dbPart.indexOf('?');
        if (q >= 0) {
            dbPart = dbPart.substring(0, q);
        }
        if (dbPart.isBlank()) {
            throw new IllegalArgumentException("Database name missing in URL: " + url);
        }

        String host = hostPort;
        int port = DEFAULT_POSTGRES_PORT;
        int colon = hostPort.lastIndexOf(':');
        if (colon > 0 && colon < hostPort.length() - 1) {
            host = hostPort.substring(0, colon);
            String portStr = hostPort.substring(colon + 1);
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port in URL: " + url);
            }
        }
        return new PostgresInfo(host, port, dbPart);
    }

    private void applyRetention(Path dir) {
        if (retentionCount < 1) {
            return;
        }
        List<DatabaseBackup> backups = databaseBackupRepository.findAllByOrderByCreatedAtDesc();
        int kept = 0;
        for (DatabaseBackup b : backups) {
            if (b.getStatus() != DatabaseBackupStatus.SUCCESS) {
                continue;
            }
            if (b.getFilePath() == null || b.getFilePath().isBlank()) {
                continue;
            }
            if (kept < retentionCount) {
                kept++;
                continue;
            }
            try {
                Path p = dir.resolve(b.getFilePath()).normalize();
                if (p.startsWith(dir)) {
                    Files.deleteIfExists(p);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private DatabaseBackupDto mapToDto(DatabaseBackup backup) {
        return DatabaseBackupDto.builder()
                .id(backup.getId())
                .status(backup.getStatus())
                .createdAt(backup.getCreatedAt())
                .startedAt(backup.getStartedAt())
                .finishedAt(backup.getFinishedAt())
                .requestedBy(backup.getRequestedBy())
                .fileName(backup.getFileName())
                .fileSize(backup.getFileSize())
                .errorMessage(backup.getErrorMessage())
                .build();
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.length() <= MAX_ERROR_LENGTH) {
            return v;
        }
        return v.substring(0, MAX_ERROR_LENGTH);
    }

    private record PostgresInfo(String host, int port, String database) {}
}

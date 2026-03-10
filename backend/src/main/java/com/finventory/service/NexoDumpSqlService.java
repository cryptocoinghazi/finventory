package com.finventory.service;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NexoDumpSqlService {

    @FunctionalInterface
    public interface InsertRowConsumer {
        void accept(List<String> columns, List<String> values) throws Exception;
    }

    public long countInsertStatements(Path dumpPath, String tableName) throws Exception {
        long count = 0L;
        String insertPrefix = "INSERT INTO `" + tableName + "`";
        try (BufferedReader reader = Files.newBufferedReader(dumpPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(insertPrefix)) {
                    count++;
                }
            }
        }
        return count;
    }

    public void forEachInsertRow(Path dumpPath, String tableName, InsertRowConsumer consumer)
            throws Exception {
        boolean inInsert = false;
        List<String> columns = null;
        String insertPrefix = "INSERT INTO `" + tableName + "`";

        try (BufferedReader reader = Files.newBufferedReader(dumpPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!inInsert) {
                    if (line.startsWith(insertPrefix)) {
                        columns = parseInsertColumns(line);
                        inInsert = true;
                    }
                    continue;
                }

                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                boolean endsSection = trimmed.endsWith(";");
                String normalized = trimmed;
                if (endsSection) {
                    normalized = normalized.substring(0, normalized.length() - 1);
                }
                normalized = normalized.stripTrailing();

                if (!normalized.isEmpty()) {
                    if (normalized.endsWith(",")) {
                        normalized = normalized.substring(0, normalized.length() - 1);
                    }

                    for (String tuple : extractTuples(normalized)) {
                        List<String> values = parseSqlTupleValues(tuple);
                        consumer.accept(columns, values);
                    }
                }

                if (endsSection) {
                    inInsert = false;
                    columns = null;
                }
            }
        }
    }

    public String getByColumn(List<String> columns, List<String> values, String column) {
        int idx = columns.indexOf(column);
        if (idx < 0 || idx >= values.size()) {
            return null;
        }
        return values.get(idx);
    }

    private List<String> parseInsertColumns(String insertHeaderLine) {
        int start = insertHeaderLine.indexOf('(');
        int end = insertHeaderLine.indexOf(')', start + 1);
        if (start < 0 || end < 0 || end <= start) {
            return List.of();
        }

        String inside = insertHeaderLine.substring(start + 1, end);
        String[] parts = inside.split(",");
        List<String> cols = new ArrayList<>(parts.length);
        for (String part : parts) {
            String c = part.trim();
            if (c.startsWith("`") && c.endsWith("`") && c.length() >= 2) {
                c = c.substring(1, c.length() - 1);
            }
            cols.add(c);
        }
        return cols;
    }

    private List<String> extractTuples(String valuesLine) {
        List<String> tuples = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        int tupleStart = -1;

        for (int i = 0; i < valuesLine.length(); i++) {
            char ch = valuesLine.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }

            if (inString) {
                if (ch == '\\') {
                    escaped = true;
                } else if (ch == '\'') {
                    inString = false;
                }
                continue;
            }

            if (ch == '\'') {
                inString = true;
                continue;
            }

            if (ch == '(') {
                if (depth == 0) {
                    tupleStart = i;
                }
                depth++;
                continue;
            }

            if (ch == ')') {
                depth--;
                if (depth == 0 && tupleStart >= 0) {
                    tuples.add(valuesLine.substring(tupleStart, i + 1));
                    tupleStart = -1;
                }
            }
        }

        return tuples;
    }

    private List<String> parseSqlTupleValues(String tuple) {
        String trimmed = tuple.trim();
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
                continue;
            }

            if (inString) {
                if (ch == '\\') {
                    escaped = true;
                    continue;
                }
                if (ch == '\'') {
                    inString = false;
                    continue;
                }
                current.append(ch);
                continue;
            }

            if (ch == '\'') {
                inString = true;
                continue;
            }

            if (ch == ',') {
                values.add(normalizeSqlValue(current.toString().trim()));
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        values.add(normalizeSqlValue(current.toString().trim()));
        return values;
    }

    private String normalizeSqlValue(String token) {
        if (token.equalsIgnoreCase("NULL")) {
            return null;
        }
        return token;
    }
}

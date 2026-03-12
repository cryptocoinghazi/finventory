package com.finventory.service;

import com.finventory.model.AuditLogEntry;
import com.finventory.repository.AuditLogEntryRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogEntryRepository auditLogEntryRepository;

    @Transactional
    public void log(String action, String entityType, UUID entityId, String details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth != null ? auth.getName() : null;
        String actorRole =
                auth != null
                        ? auth.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .findFirst()
                                .orElse(null)
                        : null;

        AuditLogEntry entry =
                AuditLogEntry.builder()
                        .actor(actor)
                        .actorRole(actorRole)
                        .action(action)
                        .entityType(entityType)
                        .entityId(entityId)
                        .details(details)
                        .build();
        auditLogEntryRepository.save(entry);
    }
}

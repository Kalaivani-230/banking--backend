package com.example.MiniProject.service;

import com.example.MiniProject.entity.AuditLog;
import com.example.MiniProject.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    public AuditLog log(Long actorId, String actorRole, String action,
                        String entityType, String entityId, String description) {
        return log(actorId, actorRole, action, entityType, entityId, description, null, null);
    }

    public AuditLog log(Long actorId, String actorRole, String action,
                        String entityType, String entityId, String description,
                        String oldValue, String newValue) {
        AuditLog a = new AuditLog();
        a.setActorId(actorId);
        a.setActorRole(actorRole);
        a.setAction(action);
        a.setEntityType(entityType);
        a.setEntityId(entityId);
        a.setDescription(description);
        a.setOldValue(oldValue);
        a.setNewValue(newValue);
        return repo.save(a);
    }

    public List<AuditLog> search(Long actorId, String action, String entityType,
                                  Instant from, Instant to) {
        return repo.search(actorId, action, entityType, from, to);
    }
}

package com.example.demo.application.shared.command;

import java.time.Instant;

import com.example.demo.application.domain.audit.aggregate.vo.DocumentOperationType;

public record RecordAuditCommand(String documentId, DocumentOperationType operationType, String beforeState,
		String afterState, Instant changedAt) {
}
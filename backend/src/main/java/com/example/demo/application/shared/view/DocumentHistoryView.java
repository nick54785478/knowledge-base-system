package com.example.demo.application.shared.view;

import java.time.Instant;

import com.example.demo.application.domain.audit.aggregate.vo.DocumentOperationType;

/**
 * 文檔歷史版本視圖
 */
public record DocumentHistoryView(
    String auditId,
    String documentId,
    DocumentOperationType operationType,
    String snapshotContent, // 當時的文檔內容 (從 afterState 解析)
    Instant changedAt
) {}
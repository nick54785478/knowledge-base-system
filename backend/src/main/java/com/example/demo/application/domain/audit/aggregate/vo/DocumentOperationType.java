package com.example.demo.application.domain.audit.aggregate.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文檔操作類型列舉
 */
@Getter
@RequiredArgsConstructor
public enum DocumentOperationType {
	CREATE("新增"), UPDATE("修改"), DELETE("刪除"), UNKNOWN("未知");

	private final String description;
}
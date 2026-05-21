package com.example.demo.application.shared.command;

import java.util.List;

/**
 * 知識文檔向量同步命令 (Command)
 * <p>
 * 封裝寫入 Elasticsearch 所需的所有核心語意資料與高維度向量。
 * </p>
 */
public record UpsertDocumentVectorCommand(
		String documentId,
		float[] vector,
		String title,
		String content,
		String category,
		String categoryName,
		List<String> tags
) {
	// Java Record 天生自帶 equals, hashCode 與 toString，非常適合作為 Command 物件
}
package com.example.demo.application.shared.view;

import java.util.List;

/**
 * 知識文檔檢索視圖 (Read Model)
 * <p>
 * 專為查詢端設計的扁平化資料結構，包含由搜尋引擎處理後的高亮片段。
 * </p>
 */
public record KnowledgeDocumentView(String id, String title, // 乾淨的原始標題 (表單編輯用)
		String highlightedTitle, // 帶有 <em> 的標題 (列表顯示用)
		String contentSnippet, // 乾淨的原始內文摘要 (表單編輯用)
		String highlightedContentSnippet, // 帶有 <em> 的內文摘要 (列表顯示用)
		String author, String category, // 分類 Code
		String categoryName, // 分類名稱
		List<String> tags) {
}
package com.example.demo.application.shared.query;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜尋知識庫的查詢指令 (Application Layer) 封裝查詢意圖，未來若需要加入分頁(page, size)或多條件篩選，可直接擴充此
 * Record
 */
public record SearchKnowledgeQuery(String keyword, String category, List<String> tags) {
	public SearchKnowledgeQuery {
		// 基礎校驗與預設值處理
		if (keyword == null) {
			keyword = "";
		}
		if (category == null) {
			category = "";
		}
		
		if (tags == null) {
			tags = new ArrayList<>();
		}
	}
}
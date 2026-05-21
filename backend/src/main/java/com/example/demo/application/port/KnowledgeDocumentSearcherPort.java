package com.example.demo.application.port;

import java.util.List;

import com.example.demo.application.shared.query.SearchKnowledgeQuery;
import com.example.demo.application.shared.view.KnowledgeDocumentView;

/**
 * 知識文檔搜尋端口 (Output Port)
 * <p>
 * 定義搜尋服務的契約，基礎建設層的適配器必須實現此介面以提供實質的搜尋能力。
 * </p>
 */
public interface KnowledgeDocumentSearcherPort {

	/**
	 * 執行全文檢索
	 *
	 * @param query {@link SearchKnowledgeQuery}
	 * @return 轉換後的 View 列表
	 */
	List<KnowledgeDocumentView> search(SearchKnowledgeQuery query);

	/**
	 * 搜尋前幾名熱門的 Tags
	 * 
	 * @return Tags
	 */
	List<String> getPopularTags();
	
}

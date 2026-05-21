package com.example.demo.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.port.KnowledgeDocumentSearcherPort;
import com.example.demo.application.shared.query.SearchKnowledgeQuery;
import com.example.demo.application.shared.view.KnowledgeDocumentView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 知識庫查詢服務 (Query Side)
 * <p>
 * 專責處理所有高頻率、高效能的資料檢索請求。 透過依賴反轉 (Dependency Inversion)，本服務只依賴抽象的 Port， 徹底與
 * Elasticsearch 或任何底層搜尋技術解耦。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeQueryService {

	private final KnowledgeDocumentSearcherPort knowledgeDocumentSearcher;

	/**
	 * 執行知識檢索並回傳高亮結果。
	 *
	 * @param query 封裝了搜尋意圖與基本校驗的應用層指令
	 * @return 轉換後的扁平化視圖 (Read Model) 列表
	 */
	@Transactional(readOnly = true)
	public List<KnowledgeDocumentView> search(SearchKnowledgeQuery query) {

		log.info("[Query Service] 準備執行檢索，關鍵字: {}", query.keyword());

		// 業務協調：將複雜的檢索任務，委派給實現了該 Port 的基礎建設 Adapter
		return knowledgeDocumentSearcher.search(query);

	}
}
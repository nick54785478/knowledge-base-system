package com.example.demo.iface.rest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.application.service.KnowledgeQueryService;
import com.example.demo.application.shared.query.SearchKnowledgeQuery;
import com.example.demo.application.shared.view.KnowledgeDocumentView;
import com.example.demo.iface.dto.res.KnowledgeSearchedResource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge-documents")
@RequiredArgsConstructor
public class KnowledgeQueryController {

	private final KnowledgeQueryService queryService;

	/**
	 * 全文檢索
	 * <p>
	 * 遵循 RESTful 規範，查詢操作使用 GET 方法，參數透過 Query String 傳遞。
	 * </p>
	 *
	 * @param keyword  搜尋關鍵字
	 * @param category 分類
	 * @return 包含高亮片段的搜尋結果
	 */
	@GetMapping("/search")
	public ResponseEntity<KnowledgeSearchedResource> searchDocuments(
			@RequestParam(name = "q", required = false, defaultValue = "") String keyword,
			@RequestParam(required = false, defaultValue = "") String category,
			@RequestParam(required = false, defaultValue = "") List<String> tags) {

		log.info("接收到檢索請求，關鍵字: {}, 分類: {}, tags: {}", keyword, category, tags);

		// 防腐與意圖封裝:
		SearchKnowledgeQuery query;
		try {
			query = new SearchKnowledgeQuery(keyword, category, tags);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(new KnowledgeSearchedResource("400", e.getMessage(), List.of()));
		}

		// 呼叫應用層服務
		List<KnowledgeDocumentView> searchResults = queryService.search(query);

		KnowledgeSearchedResource response = new KnowledgeSearchedResource("200",
				"查詢成功，共找到 " + searchResults.size() + " 筆結果", searchResults);

		return ResponseEntity.ok(response);
	}
}
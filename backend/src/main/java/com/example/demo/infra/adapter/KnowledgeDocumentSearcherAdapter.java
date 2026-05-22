package com.example.demo.infra.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.KnowledgeDocumentSearcherPort;
import com.example.demo.application.shared.query.SearchKnowledgeQuery;
import com.example.demo.application.shared.view.KnowledgeDocumentView;
import com.example.demo.infra.es.constant.KnowledgeIndexConstants;
import com.example.demo.infra.es.helper.ElasticsearchTemplateHelper;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.NamedValue;
import co.elastic.clients.util.ObjectBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch 知識文檔搜尋適配器 (Driven Adapter / Persistence Adapter)
 * <p>
 * 本類別實現了 {@link KnowledgeDocumentSearcherPort}，作為六角形架構中的輸出適配器， 負責維護 CQRS
 * 架構中的讀取模型 (Query Side) 全文檢索業務。
 * </p>
 *
 * <pre>
 * 設計亮點：
 * 1. 【動態配置注入】：透過 @Value 讀取與 Spring AI 向量庫相同的外部配置，消除 Hardcode 隱患，達成讀寫索引強一致性。
 * 2. 【防腐與解耦】：全面導入 ElasticsearchTemplateHelper 處理 I/O 異常，並透過 KnowledgeIndexConstants 統一管理欄位常數。
 * 3. 【複合布林查詢】：整合了 MultiMatch 全文檢索（含權重 Boosting 與模糊容錯）與精準的 Filter 上下文過濾。
 * 4. 【結果轉譯】：將 Elasticsearch 原始的非結構化 Hits 數據，安全轉譯為內聚的高亮檢索視圖 DTO。
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class KnowledgeDocumentSearcherAdapter implements KnowledgeDocumentSearcherPort {

	/**
	 * 通用 ES 查詢執行器 (封裝了原生的 ElasticsearchClient 與統一的例外轉譯機制)
	 */
	private final ElasticsearchTemplateHelper esHelper;

	/**
	 * 從外部配置動態注入的目標索引名稱。整合自 spring.ai.vectorstore.elasticsearch.index-name
	 */
	@Value("${spring.ai.vectorstore.elasticsearch.index-name}")
	private String indexName;

	/**
	 * 執行全文檢索、條件過濾與關鍵字高亮提取
	 *
	 * @param query 應用層傳遞的搜尋意圖查詢條件
	 * @return 包含高亮標記的文檔前端檢索視圖清單
	 */
	@Override
	public List<KnowledgeDocumentView> search(SearchKnowledgeQuery query) {
		log.info("[ES Search] 開始執行全文檢索 | 目標索引: {}, 查詢條件: {}", this.indexName, query);

		@SuppressWarnings("rawtypes")
		SearchResponse<Map> response = esHelper.executeSearch(s -> s.index(this.indexName)
				// 委派給獨立方法構建複合查詢，降低認知複雜度 (Cognitive Complexity)
				.query(q -> q.bool(b -> buildSearchConditions(b, query)))
				// 配置關鍵字高亮 (Highlighting)
				.highlight(h -> h.preTags(KnowledgeIndexConstants.HIGHLIGHT_PRE_TAG)
						.postTags(KnowledgeIndexConstants.HIGHLIGHT_POST_TAG)
						.fields(NamedValue.of(KnowledgeIndexConstants.FIELD_TITLE, HighlightField.of(f -> f)),
								NamedValue.of(KnowledgeIndexConstants.FIELD_CONTENT, HighlightField.of(f -> f))))
				.size(10), // 預設限制回傳前 10 筆最相關結果
				Map.class, "搜尋服務暫時無法使用，請稍後再試" // 自定義業務錯誤訊息
		);

		return mapToView(response);
	}

	/**
	 * 建構 Elasticsearch BoolQuery 的查詢與過濾條件
	 *
	 * @param b     BoolQuery 建造者
	 * @param query 查詢條件
	 * @return 組合完成的 BoolQuery 建造者
	 */
	private ObjectBuilder<BoolQuery> buildSearchConditions(BoolQuery.Builder b, SearchKnowledgeQuery query) {

		// 1. 處理全文檢索關鍵字 (Must Context：參與相關性算分 Score 計算)
		if (query.keyword() != null && !query.keyword().isBlank()) {
			b.must(m -> m.multiMatch(
					mm -> mm.fields(KnowledgeIndexConstants.FIELD_TITLE_BOOST, KnowledgeIndexConstants.FIELD_CONTENT)
							.query(query.keyword()).fuzziness("AUTO") // 開啟自動模糊容錯
			));
		}

		// 2. 處理分類過濾 (Filter Context：不參與算分、快取查詢結果、速度極快)
		if (query.category() != null && !query.category().isBlank() && !query.category().equals("全部")) {
			b.filter(f -> f.match(m -> m.field(KnowledgeIndexConstants.FIELD_CATEGORY).query(query.category())));
		}

		// 3. 處理標籤過濾 (Filter Context: 使用 terms 進行多選陣列匹配)
		if (query.tags() != null && !query.tags().isEmpty()) {
			List<FieldValue> tagValues = query.tags().stream().map(FieldValue::of).toList();

			b.filter(f -> f.terms(t -> t.field(KnowledgeIndexConstants.FIELD_TAGS).terms(t2 -> t2.value(tagValues))));
		}

		return b;
	}

	/**
	 * 取得系統中所有現存的標籤 (依使用頻率排序)
	 *
	 * @return 熱門標籤列表
	 */
	@Override
	public List<String> getPopularTags() {
		try {
			SearchResponse<Void> response = esHelper.executeSearch(s -> s.index(this.indexName).size(0) // 只要聚合結果，不需要原始文檔
					.aggregations(KnowledgeIndexConstants.AGG_POPULAR_TAGS,
							a -> a.terms(t -> t.field(KnowledgeIndexConstants.FIELD_TAGS).size(50) // 取前 50 個熱門標籤
							)), Void.class, "取得標籤列表失敗");

			return response.aggregations().get(KnowledgeIndexConstants.AGG_POPULAR_TAGS).sterms().buckets().array()
					.stream().map(bucket -> bucket.key().stringValue()).toList();

		} catch (Exception e) {
			log.error("[ES Search] 解析熱門標籤聚合結果失敗", e);
			return Collections.emptyList(); // 容錯處理：若聚合失敗則回傳空陣列，避免畫面崩潰
		}
	}

	/**
	 * 將 Elasticsearch 原始 Hits 結果安全映射為應用層 View DTO
	 *
	 * @param response ES 原始搜尋回覆
	 * @return 轉換完成且包含高亮片段的 KnowledgeDocumentView 清單
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<KnowledgeDocumentView> mapToView(SearchResponse<Map> response) {
		List<KnowledgeDocumentView> results = new ArrayList<>();

		for (Hit<Map> hit : response.hits().hits()) {
			Map<String, Object> source = hit.source();
			if (source == null) {
				continue;
			}

			// 1. 提取 ID：優先使用 ES 的 metadata
			String documentId = hit.id();

			// 2. 提取乾淨的原始資料 (供編輯表單使用)
			String rawTitle = String.valueOf(source.getOrDefault(KnowledgeIndexConstants.FIELD_TITLE, "無標題"));
			String rawContent = String.valueOf(source.getOrDefault(KnowledgeIndexConstants.FIELD_CONTENT, ""));
			String rawContentSnippet = rawContent.length() > 300 ? rawContent.substring(0, 300) + "..." : rawContent;

			// 3. 提取高亮片段 (供列表 UI 顯示用)：優先使用帶有 <em> 標籤的文字，若無匹配則回退至原始資料
			String highlightedTitle = hit.highlight().containsKey(KnowledgeIndexConstants.FIELD_TITLE)
					? String.join("...", hit.highlight().get(KnowledgeIndexConstants.FIELD_TITLE))
					: rawTitle;

			String highlightedContentSnippet = hit.highlight().containsKey(KnowledgeIndexConstants.FIELD_CONTENT)
					? String.join("...", hit.highlight().get(KnowledgeIndexConstants.FIELD_CONTENT))
					: rawContentSnippet;

			// 4. 提取分類代碼與中文名稱 (防呆處理：舊資料若無 categoryName 則退回使用代碼)
			String category = String.valueOf(source.getOrDefault(KnowledgeIndexConstants.FIELD_CATEGORY, "通用"));
			String categoryName = String
					.valueOf(source.getOrDefault(KnowledgeIndexConstants.FIELD_CATEGORY_NAME, category));

			// 5. 安全解析 JSON Array (Tags)
			List<String> documentTags = new ArrayList<>();
			Object tagsObj = source.get(KnowledgeIndexConstants.FIELD_TAGS);
			if (tagsObj instanceof List<?> list) {
				documentTags = list.stream().map(String::valueOf).toList();
			}

			// 6. 封裝成應用層定義的純淨 View 物件 (欄位職責完全分離)
			results.add(new KnowledgeDocumentView(documentId, rawTitle, // 乾淨的原始標題
					highlightedTitle, // 帶有 <em> 的高亮標題
					rawContentSnippet, // 乾淨的原始內文摘要
					highlightedContentSnippet, // 帶有 <em> 的高亮內文摘要
					String.valueOf(source.getOrDefault(KnowledgeIndexConstants.FIELD_AUTHOR, "未知作者")), category, // 分類代碼(INFO_SECURITY)
					categoryName, // 分類中文 (資訊安全)
					documentTags));
		}

		log.debug("[ES Search] 檢索結果轉譯完成，本次成功轉換 {} 筆資料", results.size());
		return results;
	}
}
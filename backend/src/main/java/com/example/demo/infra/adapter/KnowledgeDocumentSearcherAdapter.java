package com.example.demo.infra.adapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.KnowledgeDocumentSearcherPort;
import com.example.demo.application.shared.query.SearchKnowledgeQuery;
import com.example.demo.application.shared.view.KnowledgeDocumentView;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.NamedValue;
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
 * 1. 【動態配置注入】：透過 @Value 讀取與 Spring AI 向量庫相同的外部配置，消除 Hardcode
 * 隱患，達成讀寫索引強一致性。 
 * 2. 【複合布林查詢】：整合了 MultiMatch 全文檢索（含權重 Boosting 與模糊容錯）與精準的 Filter
 * 上下文過濾。 
 * 3. 【防腐結果轉譯】：將 Elasticsearch 原始的非結構化 Hits 數據，安全轉譯為內聚的高亮檢索視圖 DTO。
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class KnowledgeDocumentSearcherAdapter implements KnowledgeDocumentSearcherPort {

	/**
	 * Elasticsearch 官方 Java Low/High Level 核心客戶端
	 */
	private final ElasticsearchClient esClient;

	/**
	 * 從外部配置動態注入的目標索引名稱。 整合自 spring.ai.vectorstore.elasticsearch.index-name
	 * <p>
	 * 備註：非 final 欄位不會被 Lombok 的 @RequiredArgsConstructor 納入建構子， Spring 容器會在 Bean
	 * 實例化後，透過反射將配置值注入此欄位。
	 * </p>
	 */
	@Value("${spring.ai.vectorstore.elasticsearch.index-name}")
	private String indexName;

	/**
	 * 執行全文檢索、條件過濾與關鍵字高亮提取
	 *
	 * @param query 應用層傳遞的搜尋意圖查詢條件 {@link SearchKnowledgeQuery}
	 * @return 包含高亮標記的文檔前端檢索視圖清單 {@link KnowledgeDocumentView}
	 * @throws RuntimeException 當 Elasticsearch 叢集連線異常或語法解析失敗時拋出，對內封裝技術細節
	 */
	@Override
	public List<KnowledgeDocumentView> search(SearchKnowledgeQuery query) {
		log.info("[ES Search] 開始執行全文檢索 | 目標索引: {}, 查詢條件: {}", this.indexName, query);

		try {
			@SuppressWarnings("rawtypes")
			SearchResponse<Map> response = esClient.search(s -> s.index(this.indexName) // 動態使用與 Spring AI 向量庫對齊的索引名稱
					.query(q -> q.bool(b -> {

						// 1. 處理全文檢索關鍵字 (Must Context：參與相關性算分 Score 計算)
						if (query.keyword() != null && !query.keyword().isBlank()) {
							b.must(m -> m.multiMatch(mm -> mm.fields("title^3", "content") // 標題給予 3 倍權重 Boosting
									.query(query.keyword()).fuzziness("AUTO") // 開啟自動模糊容錯 (防打錯字)
							));
						}

						// 2. 處理分類過濾 (Filter Context：不參與算分、快取查詢結果、速度極快)
						if (query.category() != null && !query.category().isBlank() && !query.category().equals("全部")) {
							b.filter(f -> f.match(m -> m.field("category").query(query.category())));
						}

						// 3. 處理標籤過濾 (Filter Context: 使用 terms 進行多選陣列匹配)
						if (query.tags() != null && !query.tags().isEmpty()) {
							// 將 List<String> 轉換為 ES Client 需要的 List<FieldValue>
							List<FieldValue> tagValues = query.tags().stream().map(FieldValue::of).toList();

							b.filter(f -> f.terms(t -> t
									// 💡 重要提示：如果 ES 是動態 Mapping，字串預設會被分詞(text)。
									// 進行 terms 精準匹配時，通常需要改用 "tags.keyword"。
									// 若前端傳入標籤卻查無資料，請將下方的 "tags" 改為 "tags.keyword"。
									.field("tags").terms(t2 -> t2.value(tagValues))));
						}

						return b;
					}))
					// 4. 配置關鍵字高亮 (Highlighting) 語意標籤
					.highlight(h -> h.preTags("<em class='highlight'>").postTags("</em>").fields(
							NamedValue.of("title", HighlightField.of(f -> f)),
							NamedValue.of("content", HighlightField.of(f -> f))))
					.size(10), // 預設限制回傳前 10 筆最相關結果
					Map.class);

			return mapToView(response);

		} catch (IOException e) {
			log.error("[ES Search] Elasticsearch 執行 IO 查詢失敗，請求參數: {}", query, e);
			throw new RuntimeException("搜尋服務暫時無法使用，請稍後再試");
		}
	}

	/**
	 * 取得系統中所有現存的標籤 (依使用頻率排序) * @return Tags
	 */
	@Override
	public List<String> getPopularTags() {
		try {
			// 使用 Terms Aggregation 抽出 tags
			SearchResponse<Void> response = esClient.search(s -> s.index(this.indexName).size(0) // 我們只要聚合結果，不要原始文檔
					.aggregations("popular_tags", a -> a.terms(t -> t.field("tags") // 聚合 tags 欄位
							.size(50) // 取前 50 個熱門標籤
					)), Void.class);

			// 解析聚合結果
			return response.aggregations().get("popular_tags").sterms().buckets().array().stream()
					.map(bucket -> bucket.key().stringValue()).toList();

		} catch (IOException e) {
			log.error("[ES Search] 取得標籤列表失敗", e);
			return Collections.emptyList();
		}
	}

	/**
	 * 將 Elasticsearch 原始 Hits 結果安全映射為應用層 View DTO * @param response ES 原始搜尋回覆
	 * 
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

			// 1. 提取 ID：優先使用 ES 的 metadata (hit.id())
			String documentId = hit.id();

			// 2. 提取乾淨的原始資料 (供編輯表單使用)
			String rawTitle = String.valueOf(source.getOrDefault("title", "無標題"));
			String rawContent = String.valueOf(source.getOrDefault("content", ""));
			String rawContentSnippet = rawContent.length() > 300 ? rawContent.substring(0, 300) + "..." : rawContent;

			// 3. 提取高亮片段 (供列表 UI 顯示用)：優先使用帶有 <em> 標籤的文字，若無匹配則回退至原始資料
			String highlightedTitle = hit.highlight().containsKey("title")
					? String.join("...", hit.highlight().get("title"))
					: rawTitle;

			String highlightedContentSnippet = hit.highlight().containsKey("content")
					? String.join("...", hit.highlight().get("content"))
					: rawContentSnippet;

			// 🌟 提取分類代碼與中文名稱 (防呆處理：舊資料若無 categoryName 則退回使用代碼)
			String category = String.valueOf(source.getOrDefault("category", "通用"));
			String categoryName = String.valueOf(source.getOrDefault("categoryName", category));

			// 4. 安全解析 JSON Array (Tags)
			List<String> documentTags = new ArrayList<>();
			Object tagsObj = source.get("tags");
			if (tagsObj instanceof List<?> list) {
				documentTags = list.stream().map(String::valueOf).collect(Collectors.toList());
			}

			// 5. 封裝成應用層定義的純淨 View 物件 (欄位職責完全分離)
			results.add(new KnowledgeDocumentView(documentId, rawTitle, // 乾淨的原始標題
					highlightedTitle, // 帶有 <em> 的高亮標題
					rawContentSnippet, // 乾淨的原始內文摘要
					highlightedContentSnippet, // 帶有 <em> 的高亮內文摘要
					String.valueOf(source.getOrDefault("author", "未知作者")), category, // 🌟 分類代碼 (INFO_SECURITY)
					categoryName, // 🌟 分類中文 (資訊安全)
					documentTags));
		}

		log.debug("[ES Search] 檢索結果轉譯完成，本次成功轉換 {} 筆資料", results.size());
		return results;
	}
}
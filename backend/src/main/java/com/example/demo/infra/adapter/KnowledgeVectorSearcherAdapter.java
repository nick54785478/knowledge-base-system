package com.example.demo.infra.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.KnowledgeVectorSearcherPort;
import com.example.demo.application.shared.view.KnowledgeDocumentView;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch 向量檢索適配器 (Driven Adapter)
 * <p>
 * 實作 {@link KnowledgeVectorSearcherPort}，專司企業知識庫的語意搜尋。 底層採用 Elasticsearch 的 KNN
 * (K-Nearest Neighbors) 近似最近鄰演算法進行高維度向量比對。
 * </p>
 * 
 * <pre>
 * 維運注意 (Operation Notes): 
 * 1. 本類別強烈依賴 ES Index 的 Mapping 設定。`embedding_vector` 必須為 `dense_vector` 型別。 
 * 2. 若要使用 Tags 過濾，`tags` 欄位在 ES Mapping 中必須設定為 `keyword` 型別以支援精確比對 (Terms Query)。
 * </pre>
 */
@Slf4j
@Component
class KnowledgeVectorSearcherAdapter implements KnowledgeVectorSearcherPort {

	private final ElasticsearchClient esClient;
	private final String indexName;

	/**
	 * 建構子注入
	 * 
	 * @param esClient  Elasticsearch 官方推薦的新版 Java API Client
	 * @param indexName 從 application.properties 動態注入的向量索引名稱，方便切換環境 (dev/uat/prod)
	 */
	public KnowledgeVectorSearcherAdapter(ElasticsearchClient esClient,
			@Value("${spring.ai.vectorstore.elasticsearch.index-name}") String indexName) {
		this.esClient = esClient;
		this.indexName = indexName;
	}

	/**
	 * 執行 KNN 向量相似度檢索 (支援 Tags 預先過濾 Pre-filtering)
	 *
	 * @param queryVector 由使用者問題轉換而成的密集向量 (維度需與 ES 中 dense_vector 的 dims 一致)
	 * @param topK        預期取回的最相似文檔數量 (最終傳給 LLM 作為 Context 的數量)
	 * @param tags        使用者指定的標籤過濾條件 (可為 null 或空集合，代表不限制範圍)
	 * @return 相似知識文檔視圖清單 (若發生異常或查無資料則回傳空集合)
	 */
	@Override
	public List<KnowledgeDocumentView> searchSimilar(float[] queryVector, int topK, List<String> tags) {
		try {
			// 1. 型別適配：ES Java API Client 規定 queryVector 必須為 List<Float> 的包裝型別，
			// 故此處需將原始型別 float[] 進行轉換。(注意：勿直接使用 Arrays.asList，因為不支援 primitive type array)
			List<Float> vectorList = new ArrayList<>(queryVector.length);
			for (float v : queryVector) {
				vectorList.add(v);
			}

			// 2. 建構 KNN 檢索請求
			SearchRequest searchRequest = SearchRequest.of(s -> s.index(this.indexName).knn(k -> {
				k.field("embedding_vector").queryVector(vectorList).k(topK)
						// 維運注意 - 演算法調優參數：
						// numCandidates 控制 ES 在各個 Shard 尋找候選節點的數量。
						// 設為 topK 的 10 倍是為了在「運算效能 (CPU)」與「檢索召回率 (Recall)」之間取得平衡。
						// 若發現某些冷門知識搜不到，可嘗試調高此倍數 (如 20 倍)，但會增加檢索延遲。
						.numCandidates(topK * 10);

				// KNN Pre-filtering 機制：在計算耗時的向量距離前，先用倒排索引進行精準過濾
				if (tags != null && !tags.isEmpty()) {
					List<FieldValue> tagValues = tags.stream().map(FieldValue::of).toList();

					// 💡 維運除錯提示：如果前端有傳 Tags 但 ES 卻搜不到東西，
					// 請檢查 ES Mapping 中 tags 欄位是否為 keyword。若是 text 型別，需改為 t.field("tags.keyword")
					k.filter(f -> f.terms(t -> t.field("tags").terms(t2 -> t2.value(tagValues))));
				}
				return k;
			})
					// 維運注意 - 網路頻寬優化 (Source Filtering)：
					// 向量欄位 (embedding_vector) 通常包含上千個浮點數，佔用極大頻寬。
					// 此處強制將其從回傳的 _source 中剔除，僅取回業務需要的純文本，可大幅降低網路 I/O 與反序列化成本。
					.source(src -> src.filter(f -> f.excludes("embedding_vector"))));

			// 3. 執行檢索
			// 採用 Map.class 接收 _source 是為了保持「Schema 演進彈性 (Schema Evolution)」，
			// 避免因 ES 新增/刪除欄位導致 Java 強型別反序列化報錯。
			@SuppressWarnings("rawtypes")
			SearchResponse<Map> response = esClient.search(searchRequest, Map.class);

			// 4. 解析 Hit 結果並轉譯為應用層的 Read Model (DTO)
			return response.hits().hits().stream().map(this::mapToView).toList();

		} catch (Exception e) {
			// 優雅降級 (Graceful Degradation)：
			// 捕捉連線異常或 Mapping 錯誤並記錄日誌。回傳空集合是為了確保 RAG 主流程不會崩潰。
			// 即使檢索失敗，LLM 仍可依據系統提示詞回應「查無相關資料」，提供較佳的 UX。
			log.error("[Vector Search] Elasticsearch KNN 向量檢索失敗 | Index: {}, TopK: {}, Tags: {}", indexName, topK, tags,
					e);
			return List.of();
		}
	}

	/**
	 * 內部轉譯方法：將 Elasticsearch 的 Hit 物件轉換為扁平化的視圖 (View Record)
	 * <p>
	 * 包含防禦性設計，以相容舊版缺失欄位的資料。
	 * </p>
	 *
	 * @param hit 包含 metadata 與 _source (Map) 的單筆文件結果
	 * @return 領域層可讀的 KnowledgeDocumentView
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private KnowledgeDocumentView mapToView(Hit<Map> hit) {

		Map<String, Object> source = hit.source();
		String id = hit.id();

		// 1. 防禦性解析：使用自定義的 getOrDefault 避免舊資料缺失欄位導致的 NullPointerException
		String title = getOrDefault(source, "title", "無標題");
		String content = getOrDefault(source, "content", "");
		String author = getOrDefault(source, "author", "系統預設");

		// 分類代碼 (如: INFO_SECURITY)
		String category = getOrDefault(source, "category", "通用");
		// 分類名稱 (如: 資訊安全)。若舊資料尚未被 CDC (Change Data Capture) 同步寫入此欄位，則退回顯示代碼
		String categoryName = getOrDefault(source, "categoryName", category);

		// 2. 安全解析 JSON Array (Tags)
		List<String> documentTags = new ArrayList<>();
		Object tagsObj = source.get("tags");
		if (tagsObj instanceof List<?> list) {
			documentTags = list.stream().map(String::valueOf).collect(Collectors.toList());
		}

		// 3. 記憶體與 Prompt 優化：
		// 限制單筆知識文檔的最大擷取長度 (300 字元)，避免回傳過長文本給 LLM，
		// 防止超出 LLM 的 Context Window Token 上限，同時節省 API 計費成本。
		String contentSnippet = content.length() > 300 ? content.substring(0, 300) + "..." : content;

		// 4. 封裝 View 物件：
		// 維運注意：純 KNN 向量搜尋無法像傳統關鍵字搜尋一樣產生 `highlight` 標記。
		// 因介面需相容關鍵字檢索的格式，故在此將 highlighted 相關欄位直接退回填入原始值。
		return new KnowledgeDocumentView(id, title, // 原始標題
				title, // 向量檢索無高亮，直接填入原始標題
				contentSnippet, // 原始內文摘要
				contentSnippet, // 向量檢索無高亮，直接填入原始內文摘要
				author, category, // 用於前端判斷圖示或編輯表單的代碼
				categoryName, // 提供給 LLM 作為 Prompt Context 的友善中文名稱
				documentTags);
	}

	/**
	 * 輔助方法：安全的 Map 取值
	 *
	 * @param map          資料來源 Map
	 * @param key          欲取值的鍵名
	 * @param defaultValue 若值為 null 時的回退預設值
	 * @return 轉換為 String 的值或預設值
	 */
	private String getOrDefault(Map<String, Object> map, String key, String defaultValue) {
		if (map == null)
			return defaultValue;
		Object value = map.get(key);
		return value != null ? String.valueOf(value) : defaultValue;
	}
}
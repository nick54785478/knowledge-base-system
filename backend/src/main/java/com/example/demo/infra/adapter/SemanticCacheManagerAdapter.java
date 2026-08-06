package com.example.demo.infra.adapter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.SemanticCacheManagerPort;
import com.example.demo.infra.es.helper.ElasticsearchTemplateHelper;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 語意快取管理器 Elasticsearch 實作 (Driven Adapter)
 * 
 * <pre>
 * 使用 Elasticsearch 作為向量快取資料庫。 
 * 維運注意：請確保 ES 中存在 `qa_semantic_cache` 索引， 且 mapping 包含 `embedding_vector` (dense_vector) 
 * 與 `answer` (text/keyword)。
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SemanticCacheManagerAdapter implements SemanticCacheManagerPort {

	private final ElasticsearchTemplateHelper esHelper;

	@Value("${spring.ai.vectorstore.elasticsearch.cache-index-name:qa_semantic_cache}")
	private String cacheIndexName;

	/**
	 * 尋找語意最相似的歷史問答
	 */
	@Override
	public Optional<String> findSimilarAnswer(float[] queryVector, double threshold) {
		try {
			// 1. 型別適配：float[] 轉為 List<Float>
			List<Float> vectorList = new ArrayList<>(queryVector.length);
			for (float v : queryVector) {
				vectorList.add(v);
			}

			// 2. 執行 KNN 檢索，只取最相似的 Top 1
			@SuppressWarnings("rawtypes")
			SearchResponse<Map> response = esHelper.executeSearch(s -> s.index(cacheIndexName)
					.knn(k -> k.field("embedding_vector").queryVector(vectorList).k(1).numCandidates(10) // 快取庫不大，候選集設
																											// 10 即可
					).size(1), Map.class, "查詢語意快取失敗");

			// 3. 判斷是否有結果
			if (!response.hits().hits().isEmpty()) {
				Hit<Map> topHit = response.hits().hits().get(0);

				// 4. 【核心防禦】檢查相似度是否超過我們設定的閾值 (Threshold)
				// 唯有極度相似的問題 (如 score > 0.95)，我們才敢拿快取答案出來用
				if (topHit.score() != null && topHit.score() >= threshold) {
					Map<String, Object> source = topHit.source();
					if (source != null && source.containsKey("answer")) {
						log.info("[Semantic Cache] 🎯 快取命中！相似度 Score: {}", topHit.score());
						return Optional.of(String.valueOf(source.get("answer")));
					}
				} else {
					log.debug("[Semantic Cache] 相似度不足 (Score: {})，未達標 (Threshold: {})", topHit.score(), threshold);
				}
			}

			return Optional.empty();

		} catch (Exception e) {
			// 降級處理：快取查壞了沒關係，當作 Cache Miss，讓系統走原流程去問 LLM
			log.warn("[Semantic Cache] 查詢快取時發生異常，降級為 Cache Miss", e);
			return Optional.empty();
		}
	}

	/**
	 * 寫入新的問答快取
	 */
	@Override
	public void putCache(String question, float[] questionVector, String answer) {
		try {
			// 1. 構建要寫入的 Payload
			Map<String, Object> cacheDocument = new HashMap<>();
			cacheDocument.put("question", question); // 原提問保留作為日後查修對照
			cacheDocument.put("answer", answer);
			cacheDocument.put("created_at", Instant.now().toString());

			List<Float> vectorList = new ArrayList<>(questionVector.length);
			for (float v : questionVector) {
				vectorList.add(v);
			}
			cacheDocument.put("embedding_vector", vectorList);

			// 使用 UUID 作為 Document ID，確保快取文件的唯一性
			String docId = UUID.randomUUID().toString();

			// 2. 執行寫入
			esHelper.executeIndex(i -> i.index(cacheIndexName).id(docId).document(cacheDocument), "寫入語意快取失敗");

			log.debug("[Semantic Cache] 成功寫入新快取，提問: {}", question);

		} catch (Exception e) {
			// 寫入快取失敗不該阻擋主流程 (LLM 已經回答完了)，僅記錄日誌
			log.error("[Semantic Cache] 非同步寫入快取失敗", e);
		}
	}

	/**
	 * 清空快取實作
	 */
	@Override
	public void clearAllCache() {
		log.info("[Semantic Cache] 接收到清除指令，準備清空快取索引: {}", cacheIndexName);

		try {
			esHelper.executeDeleteByQuery(d -> d.index(cacheIndexName).query(q -> q.matchAll(m -> m)), "清空快取索引失敗");

			log.info("[Semantic Cache] 快取清空完畢，已強制重置 LLM 檢索狀態！");

		} catch (Exception e) {
			log.error("[Semantic Cache] 清空快取未預期失敗，請留意可能產生舊資料幻覺", e);
		}
	}
}
package com.example.demo.infra.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.DocumentVectorSyncHandlerPort;
import com.example.demo.application.shared.command.UpsertDocumentVectorCommand;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch 向量與屬性同步適配器 (Driven Adapter)
 * <p>
 * 實作 {@link DocumentVectorSyncHandlerPort}，負責將關聯式資料庫 (RDBMS) 的異動 或 LLM
 * 產生的向量特徵，單向同步寫入至 Elasticsearch。
 * </p>
 * <p>
 * 維運注意 (Operation Notes): 本類別採用 Upsert (Update or Insert) 語意，並支援 Partial
 * Update。 拋出的 RuntimeException 必須由上層呼叫者 (如 Kafka Listener 或 RabbitMQ Consumer)
 * 妥善攔截，以觸發死信佇列 (DLQ) 或重試機制 (Retry Policy)。
 * </p>
 */
@Slf4j
@Component
class DocumentVectorSyncHandlerAdapter implements DocumentVectorSyncHandlerPort {

	private final ElasticsearchClient esClient;
	private final String indexName;

	/**
	 * 建構子注入
	 *
	 * @param esClient  Elasticsearch 原生 Java 用戶端
	 * @param indexName 從 application.properties 動態注入的向量索引名稱
	 */
	public DocumentVectorSyncHandlerAdapter(ElasticsearchClient esClient,
			@Value("${spring.ai.vectorstore.elasticsearch.index-name}") String indexName) {
		this.esClient = esClient;
		this.indexName = indexName;
	}

	/**
	 * 執行文檔的 Upsert (新增或更新) 操作
	 * <p>
	 * 支援「屬性與向量的非同步分離寫入」。即使 LLM 產生向量失敗，只要 Metadata 發生異動， 依然能安全更新 ES
	 * 中的文字欄位，而不會洗掉原有的舊向量。
	 * </p>
	 *
	 * @param command 包含文檔元資料與(可選)向量陣列的同步指令
	 */
	@Override
	public void upsertDocument(UpsertDocumentVectorCommand command) {
		try {
			// 1. 構建要寫入/更新的 Payload (文件主體)
			Map<String, Object> docPayload = new HashMap<>();
			docPayload.put("title", command.title());
			docPayload.put("content", command.content());
			docPayload.put("category", command.category());
			docPayload.put("categoryName", command.categoryName());

			// 安全處理 List 避免 NullPointerException
			docPayload.put("tags", command.tags() == null ? new ArrayList<>() : command.tags());

			// 核心架構升級：Partial Update (部分更新) 保護機制
			// 在事件驅動 (Event-Driven) 或非同步處理的情境下，LLM 的 API 可能因限流而回傳失敗。
			// 若 command 中的 vector 為 null 或空陣列，我們就「不」把 embedding_vector 放入 payload 中。
			// 這樣 ES 在執行 Update 時，就不會把舊的合法向量覆蓋為 null，確保 RAG 檢索不會因單次同步失敗而破壞資料。
			if (command.vector() != null && command.vector().length > 0) {
				docPayload.put("embedding_vector", command.vector());
			}

			// 2. 構建 ES 更新請求 (Update Request)
			UpdateRequest<Void, Map<String, Object>> updateRequest = UpdateRequest
					.of(u -> u.index(this.indexName).id(command.documentId()).doc(docPayload)
							// 🚨 維運關鍵 - Upsert 語意：
							// true 代表若指定 ID 的文檔不存在，則將此 payload 作為新文件插入 (Insert)；若存在則合併更新 (Update)。
							.docAsUpsert(true)
							// 🚨 維運關鍵 - 樂觀鎖重試 (Optimistic Locking Retry)：
							// 在 CDC 高頻寫入的情境下，同一筆資料可能在極短時間內收到多次更新事件。
							// 若發生 Version Conflict (HTTP 409)，此參數會讓 ES 引擎在內部自動重試 3 次，大幅降低外部程式的報錯率。
							.retryOnConflict(3));

			// 3. 執行寫入
			esClient.update(updateRequest, Void.class);

			log.info("[ES Sync] 成功寫入/覆寫文檔向量與 Metadata | DocID: {}", command.documentId());

		} catch (Exception e) {
			// 4. 錯誤處理：阻斷並拋出 RuntimeException
			// 🚨 這裡不吃掉 Exception，而是包裝後拋出。
			// 目的是為了讓上層的 Spring Transaction 或 MQ 框架知道「這筆事件處理失敗了」，進而觸發重試 (Nack) 或是進入 DLQ
			// (死信佇列) 等待人工介入。
			log.error("[ES Sync] 寫入 Elasticsearch 發生嚴重異常 | DocID: {}", command.documentId(), e);
			throw new RuntimeException("Elasticsearch 向量與屬性同步失敗", e);
		}
	}
}
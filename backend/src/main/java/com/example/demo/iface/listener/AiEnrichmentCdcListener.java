package com.example.demo.iface.listener;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.example.demo.application.domain.knowledege.aggregate.KnowledgeDocument;
import com.example.demo.application.port.DocumentVectorSyncHandlerPort;
import com.example.demo.application.port.SystemDictionaryPort;
import com.example.demo.application.port.TextEmbeddingGeneratorPort;
import com.example.demo.application.shared.command.UpsertDocumentVectorCommand;
import com.example.demo.infra.debezium.translator.DebeziumEventTranslator;
import com.example.demo.infra.repository.KnowledgeDocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 知識擴充監聽器 (AI Enrichment Listener / Inbound Adapter)
 * <p>
 * 本類別在六角形架構中扮演「事件驅動的輸入適配器 (Event-Driven Inbound Adapter)」。 負責非同步監聽底層資料庫的文檔異動
 * (CDC)，透過 LLM 模型計算文本的密集向量 (Dense Vector)， 並將向量特徵與 Metadata 局部更新至 Elasticsearch
 * (Query Model)。
 * </p>
 * *
 * 
 * <pre>
 * 核心價值與架構決策：
 * 1. 【CQRS 讀寫分離】：將傳統的關聯式資料庫寫入，無縫轉換為支援 RAG (Retrieval-Augmented Generation) 的語意檢索模型。
 * 2. 【效能與成本隔離】：採用獨立的 Kafka Group ID ("ai-enrichment-group")，確保高延遲的 AI 運算不會阻塞一般稽核日誌的消費。
 * 3. 【語意防腐過濾】：透過比對異動前後的關鍵欄位 (包含標籤 tags)，過濾無效異動，大幅節省 LLM Token 運算成本。
 * 4. 【提示工程增強】：將分類(中文名稱)與標籤結構化地融入文本前綴，提升 LLM 產出向量的精準度與群聚效應。
 * </pre>
 * 
 * * @author Nick
 * 
 * @version 2.4 (整合 SystemDictionaryPort 動態查表與修復 Debezium 陷阱)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiEnrichmentCdcListener {

	private final DebeziumEventTranslator translator;
	private final TextEmbeddingGeneratorPort embeddingPort;
	private final DocumentVectorSyncHandlerPort esUpdatePort;
	private final ObjectMapper objectMapper;
	private final KnowledgeDocumentRepository knowledgeDocumentRepository;
	// 🌟 注入字典服務，用於將分類代碼轉為中文
	private final SystemDictionaryPort dictionaryPort;

	/**
	 * 消費 CDC 異動事件，並根據語意變更決定是否觸發 AI 向量化流程。
	 *
	 * @param message Debezium 傳遞的 CDC 原始 JSON 訊息 (來自 Kafka Topic)
	 */
	@KafkaListener(topics = "${app.kafka.topic.audit-log}", groupId = "ai-enrichment-group")
	public void enrichDocumentWithAi(@Payload(required = false) String message) {
		// 防禦性設計：過濾掉 Kafka 中為了日誌壓實 (Log Compaction) 所發送的 null 墓碑訊息 (Tombstone)
		if (message == null) {
			return;
		}

		translator.translate(message).ifPresent(command -> {
			switch (command.operationType()) {
			case CREATE:
				log.info("[AI Enrichment] 偵測到新文檔建立，觸發首次向量化 | DocID: {}", command.documentId());
				processEmbedding(command.documentId(), command.afterState());
				break;

			case UPDATE:
				// 🌟 核心優化：檢查核心語意欄位是否真的有變動 (防禦無效的 AI API 呼叫)
				if (!isEmbeddingRequired(command.beforeState(), command.afterState())) {
					log.info("[AI Enrichment] 檢測到非語意核心欄位異動 (如單純修改作者)，跳過 AI 向量化計算 | DocID: {}", command.documentId());
					break;
				}

				log.info("[AI Enrichment] 偵測到關鍵欄位變更 (如：標籤增減、分類移轉)，觸發重新向量化 | DocID: {}", command.documentId());
				processEmbedding(command.documentId(), command.afterState());
				break;

			case DELETE:
				// 架構決策：實體文檔的刪除已交由 Logstash 或其他的 Sync 機制處理，
				// 此處 AI 擴充模組專注於特徵生成，不需重複執行清理動作。
				break;
			default:
				break;
			}
		});
	}

	/**
	 * 執行核心的向量化與狀態同步業務。 包含資料清洗、LLM 特徵提取 (Prompt Engineering) 與 Elasticsearch 狀態覆寫
	 * (Upsert)。
	 *
	 * @param documentId 文檔的全域唯一識別碼
	 * @param afterState 異動後的 JSON 狀態字串 (代表資料庫最新狀態)
	 */
	private void processEmbedding(String documentId, String afterState) {
		try {
			JsonNode root = objectMapper.readTree(afterState);
			String title = root.path("title").asText("");
			String content = root.path("content").asText("");
			String categoryCode = root.path("category").asText("");

			// 🌟 1. 核心防禦與反查機制 (解決 PostgreSQL TOAST 欄位遺失問題)
			// 當發現內容為空，或是 Debezium 傳來保留字時，啟動資料庫反查，取代原本中斷同步的作法
			if (!root.hasNonNull("content") || content.isBlank() || content.equals("__debezium_unavailable_value")) {
				log.info("[AI Enrichment] 偵測到 TOAST 欄位遺失，啟動 DB 反查機制以補齊完整資料 | DocID: {}", documentId);

				// 透過 ID 向 PostgreSQL 索取最新、最完整的整筆資料
				KnowledgeDocument latestDoc = knowledgeDocumentRepository.findById(documentId).orElse(null);

				if (latestDoc == null) {
					log.warn("[AI Enrichment] DB 反查失敗，文檔可能已被實體刪除 | DocID: {}", documentId);
					return; // 資料已不存在，中斷流程
				}

				// 🌟 覆寫殘缺的欄位，確保拿去算 AI 向量的 Context 是 100% 完整的
				title = latestDoc.getTitle();
				content = latestDoc.getContent();
				// 根據你的 Entity 實作，取得對應的分類代碼。若是使用 String 則為 getCategory()，若是 VO 則可能為
				// getCategory().value()
				categoryCode = latestDoc.getCategory();
			}

			// 🌟 2. 查出中文名稱 (如果查不到，防呆退回使用 code)
			String categoryName = dictionaryPort.getCategoryNameByCode(categoryCode);
			if (StringUtils.isBlank(categoryName)) {
				categoryName = categoryCode;
			}

			// 🌟 3. 安全解析 Tags 陣列 (修復 Debezium jsonb 字串化陷阱)
			List<String> tags = new ArrayList<>();
			JsonNode tagsNode = root.path("tags");

			// 關鍵修復：如果 Debezium 將 jsonb 轉成了 String，我們需要把它轉回 JsonNode
			if (tagsNode.isTextual()) {
				try {
					tagsNode = objectMapper.readTree(tagsNode.asText());
				} catch (Exception e) {
					log.warn("[AI Enrichment] 無法解析 tags 字串: {}", tagsNode.asText(), e);
				}
			}

			// 現在可以安全地判斷是否為 Array 了
			if (tagsNode.isArray()) {
				for (JsonNode node : tagsNode) {
					tags.add(node.asText());
				}
			}
			String tagsString = String.join(", ", tags);

			// 4. 組合文本 (Prompt Engineering 提示工程)
			// 將 categoryName 與 tags 作為結構化前綴加入文本中，賦予 LLM 高維度上下文
			String targetText = String.format("分類：%s%n標籤：%s%n標題：%s%n內文：%s", categoryName, tagsString, title, content);

			if (targetText.isBlank()) {
				log.warn("[AI Enrichment] 文檔內容為空，跳過向量化處理 | DocID: {}", documentId);
				return;
			}

			// 🌟 5. 呼叫外部 AI 模型取得高維度密集向量 (加上獨立的 Try-Catch 防護網)
			float[] vector = null;
			try {
				vector = embeddingPort.embed(targetText);
			} catch (Exception e) {
				// 關鍵修復：攔截 LLM Timeout，確保不會阻斷 Metadata 的同步
				log.warn("[AI Enrichment] LLM API 呼叫失敗或 Timeout！將啟動降級策略，僅同步最新 Metadata 至 ES | DocID: {}", documentId);
			}

			// 🌟 6. 同步至讀取模型 (Query Side)
			// 即使 LLM 失敗 (vector 為 null)，這裡依舊會執行，確保 ES 上的 Tags 和標題永遠是最新的！
			UpsertDocumentVectorCommand command = new UpsertDocumentVectorCommand(documentId, vector, title, content,
					categoryCode, categoryName, tags);
			esUpdatePort.upsertDocument(command);

			log.info("[AI Enrichment] 已成功處理文檔狀態同步 | DocID: {}", documentId);

		} catch (Exception e) {
			log.error("[AI Enrichment] 為文檔同步 ELK 時發生未預期錯誤 | DocID: {}", documentId, e);
		}
	}

	/**
	 * 冪等性與成本優化檢測 (Idempotency & Cost Optimization)。
	 * <p>
	 * 透過解析 Debezium 提供的 Before/After 狀態，精準判斷會影響 RAG 檢索的「核心語意欄位」是否發生變化。 只要 分類
	 * (Category)、標籤 (Tags)、標題 (Title) 或內文 (Content) 任一發生變動，即視為需要重新向量化。
	 * </p>
	 *
	 * @param beforeState 異動前的 JSON 狀態
	 * @param afterState  異動後的 JSON 狀態
	 * @return true 表示語意發生改變，需重新計算 Embedding；false 表示可安全跳過
	 */
	private boolean isEmbeddingRequired(String beforeState, String afterState) {
		if (beforeState == null || afterState == null) {
			return true; // 防呆機制：若狀態遺失，安全起見預設觸發重新向量化
		}
		try {
			JsonNode before = objectMapper.readTree(beforeState);
			JsonNode after = objectMapper.readTree(afterState);

			// 🌟 1. 比較純文字欄位 (補上 author，確保作者變更也能觸發同步)
			boolean isTextChanged = !before.path("title").asText("").equals(after.path("title").asText(""))
					|| !before.path("content").asText("").equals(after.path("content").asText(""))
					|| !before.path("category").asText("").equals(after.path("category").asText(""))
					|| !before.path("author").asText("").equals(after.path("author").asText(""));

			// 🌟 2. 深度且安全的 Tags 比較 (徹底繞過 asText() 陣列空字串的雷)
			JsonNode beforeTags = before.path("tags");
			JsonNode afterTags = after.path("tags");

			// 防禦性轉譯：如果 Debezium 傳來的是字串化 JSON，先轉成真實的 ArrayNode；如果已經是 ArrayNode 就沿用
			JsonNode parsedBeforeTags = beforeTags.isTextual() ? objectMapper.readTree(beforeTags.asText())
					: beforeTags;
			JsonNode parsedAfterTags = afterTags.isTextual() ? objectMapper.readTree(afterTags.asText()) : afterTags;

			// 利用 Jackson 內建的深度比對 (Deep Equals)，它能完美辨識陣列內容的增減
			boolean isTagsChanged = !parsedBeforeTags.equals(parsedAfterTags);

			// 只要有任何一項改變，就回傳 true 觸發更新
			if (isTextChanged || isTagsChanged) {
				return true;
			} else {
				log.debug("[AI Enrichment] 偵測到無效更新，跳過 ES 同步");
				return false;
			}

		} catch (Exception e) {
			log.warn("[AI Enrichment] 比對 CDC 前後狀態失敗，預設觸發重新向量化", e);
			return true;
		}
	}
}
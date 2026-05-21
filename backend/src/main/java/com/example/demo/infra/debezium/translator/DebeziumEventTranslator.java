package com.example.demo.infra.debezium.translator;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.demo.application.domain.audit.aggregate.vo.DocumentOperationType;
import com.example.demo.application.shared.command.RecordAuditCommand;
import com.example.demo.infra.debezium.resolver.DebeziumOperationResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * CDC 事件轉換器 (Anti-Corruption Layer 防腐層)
 * <p>
 * 負責攔截並解析來自 Kafka 的 Debezium 原始 JSON 字串，將其轉換為應用層專屬的指令 (Command)。
 * 本類別的核心職責在於阻隔外部基礎設施（Debezium JSON 結構）的技術細節， 確保應用層領域邏輯不受外部變更的污染。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebeziumEventTranslator {

	private final ObjectMapper objectMapper;

	/**
	 * 將 Kafka 的原始 JSON 訊息翻譯為應用層的稽核指令
	 *
	 * @param rawMessage 來自 Kafka 的 Debezium CDC 原始 JSON 字串
	 * @return 若為有效的資料庫異動事件，則回傳封裝好的 {@link RecordAuditCommand}； 若為無效、空值或無法解析的事件（例如
	 *         Tombstone 墓碑訊息），則回傳 Optional.empty()
	 */
	public Optional<RecordAuditCommand> translate(String rawMessage) {
	    try {
	        JsonNode rootNode = objectMapper.readTree(rawMessage);
	        JsonNode dataNode = rootNode.has("payload") ? rootNode.get("payload") : rootNode;

	        if (dataNode == null || dataNode.isNull()) {
	            return Optional.empty();
	        }

	        JsonNode beforeNode = dataNode.path("before");
	        JsonNode afterNode = dataNode.path("after");

	        if (isNullOrMissing(beforeNode) && isNullOrMissing(afterNode)) {
	            return Optional.empty();
	        }

	        // 變數型別改為 DocumentOperationType
	        DocumentOperationType operationType = DebeziumOperationResolver.determineType(dataNode);

	        // 使用 == 進行列舉比對，消除魔術字串
	        if (DocumentOperationType.UNKNOWN == operationType) {
	            log.warn("無法辨識 Debezium 異動類型，略過處理: {}", rawMessage);
	            return Optional.empty();
	        }

	        String beforeState = isNullOrMissing(beforeNode) ? null : beforeNode.toString();
	        String afterState = isNullOrMissing(afterNode) ? null : afterNode.toString();

	        // 同樣改為列舉比對
	        String documentId = (DocumentOperationType.DELETE == operationType) 
	                ? beforeNode.path("id").asText()
	                : afterNode.path("id").asText();

	        long tsMs = dataNode.path("ts_ms").asLong(System.currentTimeMillis());

	        // 將乾淨的資料封裝為純業務載體 (需確保 RecordAuditCommand 構造函數已接收 Enum)
	        return Optional.of(new RecordAuditCommand(
	                documentId, 
	                operationType, 
	                beforeState, 
	                afterState,
	                Instant.ofEpochMilli(tsMs)
	        ));

	    } catch (Exception e) {
	        log.error("解析 CDC 事件失敗，無法轉換為 Application Command。原始訊息: {}", rawMessage, e);
	        return Optional.empty();
	    }
	}
	
	/**
	 * 輔助方法：判斷 JSON 節點是否為空或遺失
	 */
	private boolean isNullOrMissing(JsonNode node) {
		return node.isMissingNode() || node.isNull();
	}
}
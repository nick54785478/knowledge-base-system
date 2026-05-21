package com.example.demo.iface.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.example.demo.application.service.DocumentAuditLogCommandService;
import com.example.demo.infra.debezium.translator.DebeziumEventTranslator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 知識庫文檔 CDC 事件監聽器 (Inbound Adapter)
 * <p>
 * 本類別擔任六角形架構中的輸入適配器，負責訂閱 Kafka 中由 Debezium 派發的 PostgreSQL 異動事件流。 核心職責包含： 1.
 * 攔截原始異動訊息。 2. 協調 {@link DebeziumEventTranslator} 執行防腐處理與格式轉換。 3. 委派
 * {@link DocumentAuditLogCommandService} 執行稽核紀錄的持久化業務邏輯。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeDocumentCdcListener {

	private final DebeziumEventTranslator translator;
	private final DocumentAuditLogCommandService auditLogService;

	/**
	 * 消費並處理來自 Debezium 的文檔異動事件
	 * <p>
	 * 透過標註 {@code @Payload(required = false)} 以優雅處理 Kafka 的 Tombstone (墓碑) 訊息。
	 * 若接收到有效訊息，將透過翻譯官轉換為業務指令 (Command) 並觸發後續稽核流程。
	 * </p>
	 *
	 * @param message 來自 Kafka Topic "pg_cdc.public.knowledge_document" 的原始 JSON 字串
	 */
	@KafkaListener(topics = "${app.kafka.topic.audit-log}", groupId = "${app.kafka.group-id.audit-log}")
	public void consumeDebeziumEvent(@Payload(required = false) String message) {
		// 1. 攔截並略過 Debezium 為了日誌壓實 (Log Compaction) 所發送的 Tombstone 訊息
		if (message == null) {
			log.debug("[Audit] 偵測到 Tombstone 墓碑訊息，略過解析程序");
			return;
		}

		// 2. 透過翻譯官將基礎建設層的 JSON 訊息轉譯為純淨的業務指令 (Command)
		translator.translate(message)
				// 3. 若轉換成功且非 UNKNOWN 類型，則交由應用層服務執行持久化作業
				.ifPresent(command -> {
					auditLogService.recordAuditLog(command);
					log.info("[Audit] 異動事件已成功轉發至應用層處理 | DocID: {}", command.documentId());
				});
	}
}
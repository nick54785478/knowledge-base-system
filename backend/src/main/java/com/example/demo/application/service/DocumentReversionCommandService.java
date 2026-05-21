package com.example.demo.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.domain.audit.aggregate.DocumentAuditLog;
import com.example.demo.application.domain.knowledege.aggregate.KnowledgeDocument;
import com.example.demo.infra.repository.DocumentAuditLogRepository;
import com.example.demo.infra.repository.KnowledgeDocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 知識文檔歷史回溯應用服務 (Application Service - Command Side)
 * <p>
 * 本服務負責執行「時光機回溯」的使用案例 (Use Case)。 在設計上遵循不可變性原則：本服務不會去修改或刪除現有的歷史稽核軌跡，
 * 而是讀取過去某個特定時間點的狀態快照，並透過聚合根 (Aggregate Root) 的業務行為方法， 發起一個全新的 {@code UPDATE}
 * 指令來覆寫當前文檔。
 * </p>
 * <p>
 * 此舉不僅保護了稽核日誌的法律效力 (Append-Only)，更確保了這一次的「回溯動作」 也能被下游的 CDC (Debezium)
 * 機制正常捕捉，留存完整的操作鏈。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentReversionCommandService {

	private final ObjectMapper objectMapper;
	private final DocumentAuditLogRepository auditLogRepository;
	private final KnowledgeDocumentRepository documentRepository;

	/**
	 * 將指定的知識文檔退回至歷史上的某個特定版本
	 * <p>
	 * 執行流程包含：驗證軌跡歸屬、解析歷史 JSON 快照、加載當前聚合根、執行充血模型狀態變更、持久化至資料庫。
	 * </p>
	 *
	 * @param documentId    欲進行回溯的文檔唯一識別碼
	 * @param targetAuditId 目標歷史版本的稽核軌跡 ID
	 * @throws IllegalArgumentException 當找不到歷史軌跡、軌跡不屬於該文檔、或目標文檔不存在時拋出
	 * @throws IllegalStateException    當目標歷史版本為刪除狀態，或文檔處於不可修改狀態 (如已封存) 時拋出
	 */
	@Transactional
	public void revertToVersion(String documentId, String targetAuditId) {

		// 1. 取得歷史軌跡，並進行安全校驗 (Invariants & Security Check)
		DocumentAuditLog targetLog = auditLogRepository.findById(targetAuditId)
				.orElseThrow(() -> new IllegalArgumentException("找不到指定的歷史軌跡 | AuditID: " + targetAuditId));

		// 防禦性校驗：確保使用者沒有惡意跨文件串改歷史版本
		if (!targetLog.getDocumentId().equals(documentId)) {
			throw new IllegalArgumentException(
					"安全錯誤：該稽核軌跡不屬於此文檔！ DocID: " + documentId + ", LogDocID: " + targetLog.getDocumentId());
		}

		// 業務規則校驗：Event Sourcing 不允許直接回溯到「已刪除」的墓碑狀態，若要復原刪除應走轉增線路
		if (targetLog.getAfterState() == null) {
			throw new IllegalStateException("無法退回至已刪除的狀態！該版本代表文檔已被刪除。");
		}

		try {
			// 2. 解析歷史狀態 (防腐處理：自歷史 JSON 快照中還原當時的資料欄位)
			JsonNode oldState = objectMapper.readTree(targetLog.getAfterState());
			String oldTitle = oldState.path("title").asText();
			String oldContent = oldState.path("content").asText();

			// 3. 取得當前的文檔聚合根 (Aggregate Root)
			KnowledgeDocument document = documentRepository.findById(documentId)
					.orElseThrow(() -> new IllegalArgumentException("目標文檔不存在，無法執行回溯 | DocID: " + documentId));

			// 4. 透過領域實體的行為方法來改變狀態 (Rich Domain Model 充血模型實踐)
			// 將覆寫邏輯與不變性約束 (如：封存文檔不可修改) 內聚在實體內部，杜絕 Anemic Domain Model (貧血模型)
			document.updateContent(oldTitle, oldContent);

			// 5. 執行持久化 (存檔後 PostgreSQL WAL 會變更，Debezium 將非同步捕捉並生成一筆新的「REVERT」稽核紀錄)
			documentRepository.save(document);

			log.info("⏳ [Time Travel] 文檔 {} 已成功回退至歷史軌跡版本 {}", documentId, targetAuditId);

		} catch (Exception e) {
			// 將技術細節異常 (如 Jackson 解析失敗) 進行防腐隔離，轉換為業務級別的 RuntimeException 向上拋出
			log.error("還原歷史狀態失敗，文檔 ID: {}, 軌跡 ID: {}", documentId, targetAuditId, e);
			throw new RuntimeException("時光機回溯失敗，請聯絡系統管理員", e);
		}
	}
}
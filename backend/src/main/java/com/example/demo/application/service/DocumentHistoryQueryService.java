package com.example.demo.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.shared.view.DocumentHistoryView;
import com.example.demo.infra.repository.DocumentAuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文檔歷史軌跡查詢服務 (Query Service)
 * <p>
 * 在 CQRS 架構中，此服務專職負責「讀取 (Query)」操作。 提供知識文檔的歷史版本 (時光機) 查詢功能，將底層的 Audit Log
 * 實體轉換為前端所需的 View DTO， 並將複雜的 JSON 狀態解析為易於呈現的快照內容。
 * </p>
 */
@Slf4j
@Service
@Transactional(readOnly = true) // 💡 標記唯讀交易，提升資料庫層級的查詢效能
@RequiredArgsConstructor
public class DocumentHistoryQueryService {

	private final DocumentAuditLogRepository auditLogRepository;
	private final ObjectMapper objectMapper;

	/**
	 * 讀取指定文檔的歷史時光機 (依異動時間由早到晚排序)
	 *
	 * @param documentId 文檔的唯一識別碼
	 * @return 包含歷史狀態快照的 View DTO 清單，時間順序為 Ascending (舊 -> 新)
	 */
	public List<DocumentHistoryView> getDocumentHistory(String documentId) {

		// 1. 查詢資料庫：透過 Asc (Ascending) 確保時間軸是由過去走向現在（由早到晚）
		return auditLogRepository.findByDocumentIdOrderByChangedAtAsc(documentId).stream().map(auditLog -> {
			// 2. 預設快照狀態：針對 DELETE 操作 (因為 DELETE 的 afterState 為空)
			String snapshot = "文檔已刪除";

			// 3. 解析歷史狀態：若存在 afterState (CREATE 或 UPDATE)，則提取其內容
			if (auditLog.getAfterState() != null) {
				try {
					JsonNode afterNode = objectMapper.readTree(auditLog.getAfterState());
					// 從 JSON 節點中安全地提取 content 欄位，若無則回傳空字串
					snapshot = afterNode.path("content").asText("");
				} catch (Exception e) {
					// 防禦性設計：若 JSON 解析失敗，不應中斷整個查詢，而是給予提示文字與 Log
					snapshot = "無法解析歷史狀態";
					log.warn("無法解析 AuditLog 的歷史狀態 JSON | AuditID: {}", auditLog.getId(), e);
				}
			}

			// 4. 封裝並回傳不可變的視圖 (View) DTO，隔絕底層實體直接暴露給前端
			return new DocumentHistoryView(auditLog.getId(), auditLog.getDocumentId(), auditLog.getOperationType(),
					snapshot, auditLog.getChangedAt());
		}).toList();
	}
}
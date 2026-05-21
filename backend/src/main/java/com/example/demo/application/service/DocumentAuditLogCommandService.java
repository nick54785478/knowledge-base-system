package com.example.demo.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.domain.audit.aggregate.DocumentAuditLog;
import com.example.demo.application.shared.command.RecordAuditCommand;
import com.example.demo.infra.repository.DocumentAuditLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 知識文檔稽核軌跡應用服務 (Application Service)
 * <p>
 * 本服務擔任應用層的指揮官，負責協調領域模型（Domain Model）與持久化適配器（Persistence Adapter）。
 * 主要職責為接收來自不同入口（如 Kafka Listener 或 REST Controller）的業務指令， 並確保稽核軌跡的記錄過程具備事務一致性
 * (Transactional Consistency)。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAuditLogCommandService {

	/**
	 * 持久化端口，用於與資料庫進行交互
	 */
	private final DocumentAuditLogRepository auditLogRepository;

	/**
	 * 執行稽核紀錄持久化任務
	 * <p>
	 * 透過 {@link RecordAuditCommand} 傳遞的資訊，利用領域實體的工廠方法建立稽核對象，
	 * 並確保整個存檔動作在事務邊界內執行。若持久化失敗，事務將自動回滾。
	 * </p>
	 *
	 * @param command 包含稽核所需完整數據的應用層指令
	 */
	@Transactional
	public void recordAuditLog(RecordAuditCommand command) {

		// 1. 翻譯：將技術無關的 Command 轉換為具備業務約束的領域實體 (Domain Entity)
		// 這裡遵循 Aggregate 規則，調用靜態工廠方法 record() 以確保對象建立的合法性
		DocumentAuditLog auditLog = DocumentAuditLog.recordLog(command.documentId(), command.operationType(),
				command.beforeState(), command.afterState(), command.changedAt());

		// 2. 持久化：調用 Repository 接口將稽核對象保存至 Source of Truth (PostgreSQL)
		auditLogRepository.save(auditLog);

		// 3. 記錄日誌：僅用於開發追蹤與系統監控，不應包含敏感業務數據
		log.info("[Audit Service] 成功記錄文檔異動軌跡 | 操作: {} | DocID: {}", command.operationType(), command.documentId());
	}
}
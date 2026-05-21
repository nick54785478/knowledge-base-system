package com.example.demo.application.domain.audit.aggregate;

import java.time.Instant;
import java.util.UUID;

import com.example.demo.application.domain.audit.aggregate.vo.DocumentOperationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 知識文檔稽核軌跡實體 (Audit Log Entity)
 * <p>
 * 專門負責記錄知識文檔的每一次 CRUD 異動生命週期。 本表具備不可變性 (Immutable)，採用 Append-Only (只寫不讀/只增不減)
 * 模式， 絕對不允許進行 UPDATE 或 DELETE 操作，以確保資料的防篡改性與法律稽核效力。
 * </p>
 * 
 * <pre>
 * 維運與 DBA 注意事項 (Operation & DBA Notes): 
 * 1. 索引建議：由於常需查詢某份文件的歷史軌跡，強烈建議在資料庫為 `document_id` 與 `changed_at` 建立複合索引 (Composite Index)。 
 * 2. 容量管控：此為高頻寫入的流水帳表，資料量將無限制增長。建議定期執行資料封存 (Data Archiving) 或設定 Table Partitioning (如按月分區)。
 * </pre>
 */
@Entity
@Getter
@Table(name = "knowledge_document_audit_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 配合 JPA 規範保留無參數建構子，但設為 PROTECTED 防止業務邏輯誤用
public class DocumentAuditLog {

	/**
	 * 稽核紀錄的全局唯一識別碼 (UUID)
	 */
	@Id
	@Column(length = 36)
	private String id;

	/**
	 * 關聯的知識文檔 ID (聚合根 ID)
	 */
	@Column(name = "document_id", nullable = false, length = 36)
	private String documentId;

	/**
	 * 資料操作類型 (例如：CREATE, UPDATE, DELETE)
	 * <p>
	 * 架構設計：使用 @Enumerated(EnumType.STRING) 將 Enum 以字串形式存入資料庫。 雖然犧牲了極少數的儲存空間，但大幅提升了
	 * DBA 直接看 DB 時的可讀性， 並避免未來 Enum 順序調換時引發的嚴重資料錯亂。
	 * </p>
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "operation_type", nullable = false, length = 20)
	private DocumentOperationType operationType;

	/**
	 * 異動前的狀態 Snapshot (JSON 格式)
	 * <p>
	 * 架構設計：將物件狀態序列化為 JSON 儲存，實作了「無 Schema 限制 (Schema-less)」的彈性。
	 * 未來知識文檔若新增欄位，此表結構完全無需修改 (No DDL required)，完美適應業務的高速迭代。 若為 CREATE 操作，此欄位通常為
	 * null 或空 JSON。
	 * </p>
	 */
	@Column(name = "before_state", columnDefinition = "TEXT")
	private String beforeState;

	/**
	 * 異動後的狀態 Snapshot (JSON 格式) 若為 DELETE 操作，此欄位通常為 null。
	 */
	@Column(name = "after_state", columnDefinition = "TEXT")
	private String afterState;

	/**
	 * 異動發生的精確時間點 (UTC)
	 */
	@Column(name = "changed_at", nullable = false, updatable = false)
	private Instant changedAt;

	/**
	 * 私有化建構子 (Private Constructor)
	 * <p>
	 * 禁止外部直接使用 new 關鍵字實例化，強制開發者透過具備語意化的靜態工廠方法來建立物件， 確保領域物件在被建立的瞬間，狀態就是合法且完整的。
	 * </p>
	 */
	private DocumentAuditLog(String documentId, DocumentOperationType operationType, String beforeState,
			String afterState, Instant changedAt) {
		// 在物件建立時自動賦予 UUID，確保 ID 產生機制的統一性
		this.id = UUID.randomUUID().toString();
		this.documentId = documentId;
		this.operationType = operationType;
		this.beforeState = beforeState;
		this.afterState = afterState;
		this.changedAt = changedAt;
	}

	/**
	 * 靜態工廠方法 (Static Factory Method)：建立一筆新的稽核日誌
	 *
	 * @param documentId    異動的文檔 ID
	 * @param operationType 操作類型 (CREATE/UPDATE/DELETE)
	 * @param beforeState   異動前狀態 (JSON)，若是新增則傳入 null 或 "{}"
	 * @param afterState    異動後狀態 (JSON)，若是刪除則傳入 null 或 "{}"
	 * @param changedAt     異動時間 (建議由應用服務層的統一時鐘 Clock 傳入)
	 * @return 狀態合法且完整的 DocumentAuditLog 實體
	 */
	public static DocumentAuditLog recordLog(String documentId, DocumentOperationType operationType, String beforeState,
			String afterState, Instant changedAt) {

		// 領域防護網 (Invariants Check)：在此處阻擋不合法的資料
		if (documentId == null || documentId.isBlank()) {
			throw new IllegalArgumentException("建立稽核日誌失敗：文檔 ID 不可為空");
		}
		if (operationType == null) {
			throw new IllegalArgumentException("建立稽核日誌失敗：操作類型不可為 null");
		}

		return new DocumentAuditLog(documentId, operationType, beforeState, afterState, changedAt);
	}
}
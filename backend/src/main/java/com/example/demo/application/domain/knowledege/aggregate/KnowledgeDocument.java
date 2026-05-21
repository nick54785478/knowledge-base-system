package com.example.demo.application.domain.knowledege.aggregate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.example.demo.application.domain.knowledege.aggregate.vo.DocumentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 知識庫文檔實體 (Command 端聚合根)
 * <p>
 * 負責維護知識庫文檔的核心業務邏輯與狀態一致性。 在 CQRS 架構中，此實體僅負責「寫入 (Command)」邏輯， 作為唯一真相來源 (Source
 * of Truth) 儲存於 PostgreSQL 中。 狀態變更後，將交由底層 CDC (Debezium) 非同步同步至 Elasticsearch。
 * </p>
 *
 */
@Entity
@Table(name = "knowledge_document")
@Getter // 只提供 Getter，拒絕 Setter，保護物件狀態
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KnowledgeDocument {

	/**
	 * 全域唯一識別碼 (UUID)
	 */
	@Id
	@Column(name = "id", length = 36)
	private String id;

	/**
	 * 文檔標題
	 */
	@Column(name = "title", nullable = false, length = 255)
	private String title;

	/**
	 * 文檔內文 (支援長文本)
	 */
	@Column(name = "content", columnDefinition = "TEXT")
	private String content;

	/**
	 * 作者名稱或 ID
	 */
	@Column(length = 100)
	private String author;

	/**
	 * 文檔分類 (如: TECH_SPEC, HR_POLICY)
	 */
	@Column(length = 50)
	private String category;

	/**
	 * 知識標籤 (輕量級字串陣列) 透過 Hibernate 6 的 @JdbcTypeCode 輕鬆對應 PostgreSQL 的 jsonb 欄位
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb", name = "tags")
	private List<String> tags = new ArrayList<>();

	/**
	 * 文檔當前業務狀態
	 */
	@Column(length = 50)
	@Enumerated(EnumType.STRING)
	private DocumentStatus status;

	/**
	 * 建立時間 (寫入後即不可變更)
	 */
	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	/**
	 * 最後更新時間
	 */
	@Column(name = "updated_at")
	private Instant updatedAt;

	/**
	 * 私有建構子，強迫開發者統一使用靜態工廠方法進行實例化。
	 * 
	 * @param title    標題
	 * @param content  內容
	 * @param author   作者
	 * @param category 類別
	 * @param tags     標籤
	 * 
	 */
	private KnowledgeDocument(String title, String content, String author, String category, List<String> tags) {
		/**
		 * 在 DDD 的觀念裡，聚合根 (Aggregate Root) 在「被創建出來的那一刻」就應該具有生命與唯一識別
		 * (Identity)，而不是「等到存進資料庫 (Flush) 時」才由 Hibernate 賦予它 ID。
		 * 所以我們避免使用 @GeneratedValue(strategy = GenerationType.UUID)
		 */
		this.id = UUID.randomUUID().toString();
		this.title = title;
		this.content = content;
		this.author = author;
		this.category = category;
		this.tags = tags == null ? new ArrayList<>() : tags;
		this.status = DocumentStatus.DRAFT; // 預設為草稿
		this.createdAt = Instant.now();
		this.updatedAt = this.createdAt;
	}

	/**
	 * 創建一份新的知識庫草稿。
	 *
	 * @param title    文檔標題 (不可為空)
	 * @param content  文檔內文
	 * @param author   作者
	 * @param category 分類
	 * @param tags     標籤
	 * @return 初始化狀態的 KnowledgeDocument 實體
	 * @throws IllegalArgumentException 當標題為空時拋出
	 */
	public static KnowledgeDocument createDraft(String title, String content, String author, String category,
			List<String> tags) {
		if (title == null || title.isBlank()) {
			throw new IllegalArgumentException("文章標題不能為空！");
		}
		return new KnowledgeDocument(title, content, author, category, tags);
	}

	/**
	 * 更新 Tags 標籤
	 * 
	 * @param newTags 標籤
	 */
	public void updateTags(List<String> newTags) {
		this.tags = newTags == null ? new ArrayList<>() : newTags;
	}

	/**
	 * 發布文檔。
	 * <p>
	 * 將文檔狀態變更為 PUBLISHED，允許下游搜尋引擎進行檢索。
	 * </p>
	 */
	public void publish() {
		if (this.status == DocumentStatus.PUBLISHED) {
			return;
		}
		this.status = DocumentStatus.PUBLISHED;
		this.updatedAt = Instant.now();
	}

	/**
	 * 修訂文檔內容。
	 *
	 * @param newTitle   新標題 (若傳 null 則保持原值)
	 * @param newContent 新內文 (若傳 null 則保持原值)
	 * @throws IllegalStateException 若文檔已被封存，則拒絕修改
	 */
	public void revise(String newTitle, String newContent) {
		if (this.status == DocumentStatus.ARCHIVED) {
			throw new IllegalStateException("已封存的文章無法修改！");
		}
		this.title = newTitle != null ? newTitle : this.title;
		this.content = newContent != null ? newContent : this.content;
		this.updatedAt = Instant.now();
	}

	/**
	 * 覆寫文檔內容。
	 * <p>
	 * 具備「完整覆寫 (Full Overwrite)」的語意，可用於時光機回溯或系統級別的強制更新。 與 {@code revise}
	 * 的差異在於：此方法強制要求必須提供完整的標題與內容， 以確保狀態回滾或覆寫後的資料完整性。
	 * </p>
	 *
	 * @param targetTitle   目標標題 (不可為空)
	 * @param targetContent 目標內文 (若無內文可傳入空字串，但不可為 null)
	 * @throws IllegalArgumentException 若標題為空、或內容傳入 null
	 * @throws IllegalStateException    若文檔已被封存，則拒絕修改
	 */
	public void updateContent(String targetTitle, String targetContent) {
		// 1. 業務規則校驗 (Invariants Check)
		if (this.status == DocumentStatus.ARCHIVED) {
			throw new IllegalStateException("已封存的文章無法修改！");
		}
		if (targetTitle == null || targetTitle.isBlank()) {
			throw new IllegalArgumentException("文章標題不能為空！");
		}
		if (targetContent == null) {
			throw new IllegalArgumentException("文章內容不可為 null (若無內容請傳入空字串)！");
		}

		// 2. 狀態變更 (State Mutation)
		this.title = targetTitle;
		this.content = targetContent;

		// 3. 更新審計時間
		this.updatedAt = Instant.now();
	}

	/**
	 * 領域行為：變更分類
	 * 
	 * @param newCategory 新分類
	 */
	public void changeCategory(String newCategory) {
		// 防呆：如果分類根本沒變，就不做任何事，避免浪費 AI 算力
		if (this.category.equals(newCategory)) {
			return;
		}
		this.category = newCategory;
	}
}

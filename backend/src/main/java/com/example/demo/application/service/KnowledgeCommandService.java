package com.example.demo.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.domain.knowledege.aggregate.KnowledgeDocument;
import com.example.demo.application.port.SemanticCacheManagerPort;
import com.example.demo.application.shared.command.ChangeDocumentCategoryCommand;
import com.example.demo.application.shared.command.CreateKnowledgeCommand;
import com.example.demo.application.shared.command.UpdateKnowledgeCommand;
import com.example.demo.infra.repository.KnowledgeDocumentRepository;
import com.example.demo.infra.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 知識庫命令服務 (Command Side)
 * 
 * <pre>
 *   專責處理所有會改變系統狀態的寫入操作 (Create, Update, Delete)。 
 * 在此服務中，僅與關聯式資料庫互動，絕不直接呼叫搜尋引擎。
 * 資料的一致性由關聯式資料庫的 Transaction 保證。
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeCommandService {

	private final KnowledgeDocumentRepository repository;
	private final SemanticCacheManagerPort semanticCachePort;
	
	/**
	 * 建立並直接發布新的知識庫文檔。
	 * 
	 * <pre>
	 * 執行流程： 
	 * 1. 透過工廠方法產生聚合根實體。 
	 * 2. 執行業務邏輯 (變更狀態為發布)。 
	 * 3. 觸發 Repository 寫入資料庫。 
	 * 4. 寫入 Commit 後，Debezium 將自動擷取 WAL 紀錄並派發至下游系統。
	 * </pre>
	 *
	 * @param command {@link CreateKnowledgeCommand}
	 * @return 新建立的文檔 UUID
	 */
	@Transactional
	public String createDocument(CreateKnowledgeCommand command) {

		KnowledgeDocument document = KnowledgeDocument.createDraft(command.title(), command.content(), command.author(),
				command.category(), command.tags());

		document.publish();
		repository.save(document);
		log.info("Command: 成功寫入，ID: {}", document.getId());
		
		// 🌟 觸發快取失效 (Cache Invalidation)
        semanticCachePort.clearAllCache();
		return document.getId();
	}

	@Transactional
	public void changeCategory(String docId, ChangeDocumentCategoryCommand command) {
		// 1. 取得 Aggregate Root
		KnowledgeDocument document = repository.findById(docId)
				.orElseThrow(() -> new ResourceNotFoundException("ENTITY_NOT_FOUND", "找不到該文檔"));

		// 2. 呼叫領域行為 (由 Entity 自己檢查狀態與發布事件)
		document.changeCategory(command.newCategory());

		// 3. 儲存 (若使用 Spring Data，此時會觸發 Domain Event)
		repository.save(document);
	}

	/**
	 * 修改文檔
	 * 
	 * @param id      DocId
	 * @param command {@link UpdateKnowledgeCommand}
	 */
	@Transactional
	public void updateDocument(String id, UpdateKnowledgeCommand command) {
		// 1. 從資料庫重建聚合根 (Reconstitute Aggregate)
		KnowledgeDocument document = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("ENTITY_NOT_FOUND", "找不到指定的文檔 ID: " + id));

		// 2. 委派給聚合根執行業務邏輯與狀態變更
		document.revise(command.title(), command.content());

		// 3. 更新 Tags
		document.updateTags(command.tags());

		// 4. JPA 髒檢查 (Dirty Checking) 會在 Transaction 結束時自動 update，
		// 但為了語意明確，我們依然呼叫 save。
		repository.save(document);
		log.info("Command: 成功更新 PostgreSQL，ID: {}", document.getId());
		
		// 🌟 觸發快取失效 (Cache Invalidation)
        semanticCachePort.clearAllCache();
	}

	/**
	 * 刪除文檔 (硬刪除)
	 * 
	 * @param id DocId
	 */
	@Transactional
	public void deleteDocument(String id) {
		if (!repository.existsById(id)) {
			throw new ResourceNotFoundException("ENTITY_NOT_FOUND", "找不到指定的文檔 ID: " + id);
		}

		// 執行物理刪除。
		// 這將觸發 Debezium 產生 op="d" 的事件，
		// Logstash 收到後會向 Elasticsearch 發送 action => "delete" 的指令！
		repository.deleteById(id);
		log.info("Command: 成功從 PostgreSQL 刪除，ID: {}", id);
	}
}
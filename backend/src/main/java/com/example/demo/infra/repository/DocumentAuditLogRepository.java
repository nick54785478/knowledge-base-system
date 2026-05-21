package com.example.demo.infra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.application.domain.audit.aggregate.DocumentAuditLog;

public interface DocumentAuditLogRepository extends JpaRepository<DocumentAuditLog, String> {

	/**
	 * 取得文檔的所有歷史紀錄，依照異動時間由早到晚 (Ascending) 排序
	 */
	List<DocumentAuditLog> findByDocumentIdOrderByChangedAtAsc(String documentId);

}

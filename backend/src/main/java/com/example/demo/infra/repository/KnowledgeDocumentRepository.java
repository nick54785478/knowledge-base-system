package com.example.demo.infra.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.application.domain.knowledege.aggregate.KnowledgeDocument;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {
}
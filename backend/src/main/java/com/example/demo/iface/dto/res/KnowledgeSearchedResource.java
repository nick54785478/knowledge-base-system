package com.example.demo.iface.dto.res;

import java.util.List;

import com.example.demo.application.shared.view.KnowledgeDocumentView;

/**
 * 知識庫查詢回應載體 (Envelope Pattern) 統一前後端溝通格式，封裝從 Elasticsearch 撈出來的扁平化 JSON 資料
 */
public record KnowledgeSearchedResource(String code, String message, List<KnowledgeDocumentView> data) {
}
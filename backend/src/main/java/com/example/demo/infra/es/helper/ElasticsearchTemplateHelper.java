package com.example.demo.infra.es.helper;

import java.io.IOException;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.example.demo.infra.shared.exception.KnowledgeSearchException;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.ObjectBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch 通用查詢執行器 負責收斂重複的 try-catch 結構、統一例外轉譯，並降低 Adapter 的耦合度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchTemplateHelper {

	private final ElasticsearchClient esClient;

	/**
	 * 安全執行 ES 搜尋請求
	 *
	 * @param requestBuilder 搜尋請求建造者函數
	 * @param documentClass  預期映射的目標型別 (通常為 Map.class)
	 * @param errorMessage   發生異常時拋出的業務錯誤訊息
	 * @return 搜尋結果 Response
	 */
	public <T> SearchResponse<T> executeSearch(
			Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> requestBuilder, Class<T> documentClass,
			String errorMessage) {
		try {
			return esClient.search(requestBuilder, documentClass);

		} catch (ElasticsearchException e) {
			// 處理 ES 引擎層級的業務錯誤 (例如：Mapping 不符、索引不存在 404、語法錯誤 400)
			// e.response().error().type() 可以精準抓出 ES 吐出來的錯誤類型
			log.error("[ES 引擎異常] {} | 錯誤類型: {} | 錯誤原因: {}", errorMessage, e.response().error().type(),
					e.response().error().reason(), e);
			throw new KnowledgeSearchException(errorMessage + " (引擎處理失敗)", e);

		} catch (IOException e) {
			// 處理基礎設施網路層級的錯誤 (例如：ES 伺服器斷線、Connection Timeout)
			log.error("[ES 網路異常] {} | 可能是叢集連線失敗或 Timeout", errorMessage, e);
			throw new KnowledgeSearchException(errorMessage + " (網路連線異常)", e);

		} catch (Exception e) {
			// 兜底攔截：捕捉反序列化或其他未知的 RuntimeException
			log.error("[ES 未知異常] {}", errorMessage, e);
			throw new KnowledgeSearchException(errorMessage + " (系統發生未知錯誤)", e);
		}
	}
}

package com.example.demo.application.port;

import com.example.demo.application.shared.command.UpsertDocumentVectorCommand;

/**
 * 文檔向量與詮釋資料同步端口 (Outbound Port)
 * 
 * <pre>
 * - 在六角形架構中扮演「輸出適配器」的介面。
 * - 負責將生成的 AI 向量特徵與核心語意資料寫入至檢索端 (Query Side)。
 * </pre>
 */
public interface DocumentVectorSyncHandlerPort {

	/**
	 * 寫入或覆寫文檔的向量特徵與核心語意資料 (Upsert)
	 * <p>
	 * 採用 Doc-as-Upsert 模式，此方法將成為 Query Model 在 RAG 檢索維度上的「唯一寫入來源」。
	 * </p>
	 * 
	 * @param command 包含文檔 ID、向量陣列與所有 Metadata 的寫入命令
	 */
	void upsertDocument(UpsertDocumentVectorCommand command);
}
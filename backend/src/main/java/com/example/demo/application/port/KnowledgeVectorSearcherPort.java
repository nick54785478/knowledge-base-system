package com.example.demo.application.port;

import java.util.List;

import com.example.demo.application.shared.view.KnowledgeDocumentView;

/**
 * 知識庫向量檢索端口 (Outbound Port)
 */
public interface KnowledgeVectorSearcherPort {

	/**
	 * 執行 KNN (K-Nearest Neighbors) 向量相似度搜尋
	 *
	 * @param queryVector 使用者問題的向量陣列
	 * @param topK        要取回的最高相關度筆數
	 * @param tags        Tags
	 * @return 包含標題與內文的文檔視圖清單
	 */
	List<KnowledgeDocumentView> searchSimilar(float[] queryVector, int topK, List<String> tags);
}

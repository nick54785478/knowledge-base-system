package com.example.demo.application.port;

import java.util.Optional;

/**
 * 語意快取管理器 (Outbound Port)
 * <p>
 * 負責管理 LLM 歷史問答的向量快取，降低 API 呼叫成本與延遲。 定義了快取的讀取、寫入與全域清除操作。
 * </p>
 */
public interface SemanticCacheManagerPort {

	/**
	 * 尋找語意最相似的歷史問答
	 *
	 * @param queryVector 使用者提問的向量陣列
	 * @param threshold   相似度門檻值 (例如 0.95)
	 * @return 若命中快取則回傳歷史答案，否則回傳 empty
	 */
	Optional<String> findSimilarAnswer(float[] queryVector, double threshold);

	/**
	 * 寫入新的問答快取
	 *
	 * @param question       使用者提問 (原文)
	 * @param questionVector 提問的向量陣列
	 * @param answer         LLM 生成的回答
	 */
	void putCache(String question, float[] questionVector, String answer);

	/**
	 * 清空所有問答快取
	 * <p>
	 * 當知識庫發生異動 (CRUD) 時觸發，確保下一位使用者提問時會強制重新檢索，避免發生 AI 幻覺。
	 * </p>
	 */
	void clearAllCache();
}
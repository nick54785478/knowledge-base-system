package com.example.demo.application.port;

/**
 * 文本向量生成器端口 (Outbound Port)
 * 
 * <pre>
 * 負責將純文本轉換為高維度向量 (Dense Vector)。 
 * 本端口隔離了底層 AI 模型 (如 Ollama, OpenAI) 的技術細節，使核心業務邏輯不受具體 AI 供應商的綁架。
 * </pre>
 */
public interface TextEmbeddingGeneratorPort {

	/**
	 * 生成文本的向量表示
	 * 
	 * @param text 要轉換的文本內容 (實務上通常為標題與內文的組合)
	 * @return 浮點數陣列形式的向量 (維度大小取決於具體使用的模型，例如 nomic-embed-text 為 768 維)
	 */
	float[] embed(String text);
}
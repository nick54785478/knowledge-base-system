package com.example.demo.infra.adapter;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.TextEmbeddingGeneratorPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 本地端 LLM 向量化適配器 (Driven Adapter)
 * 
 * <pre>
 * 實作 {@link TextEmbeddingGeneratorPort}，底層封裝了 Spring AI 框架。 
 * 透過 application.properties 動態綁定至本地端的 Ollama 服務進行推論。 
 * 類別可見性設為 Package-Private，嚴格防止核心業務層直接依賴此具體實作。
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class TextEmbeddingGenerator implements TextEmbeddingGeneratorPort {

	/**
	 * Spring AI 提供的核心介面，自動讀取環境變數配置 (如: Ollama URL 與模型名稱) 進行注入
	 */
	private final EmbeddingModel embeddingModel;

	/**
	 * 生成文本的向量表示
	 * 
	 * @param text 要轉換的文本內容 (實務上通常為標題與內文的組合)
	 * @return 浮點數陣列形式的向量 (維度大小取決於具體使用的模型，例如 nomic-embed-text 為 768 維)
	 */
	@Override
	public float[] embed(String text) {
		log.debug("正在呼叫本地端 Ollama (Spring AI) 進行向量計算...");
		// 委派給 Spring AI 執行 HTTP 請求與 JSON 轉換，直接回傳 float[]
		return embeddingModel.embed(text);
	}
}
package com.example.demo.application.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.demo.application.port.KnowledgeVectorSearcherPort;
import com.example.demo.application.port.TextEmbeddingGeneratorPort;
import com.example.demo.application.shared.view.KnowledgeDocumentView;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 企業級 RAG (Retrieval-Augmented Generation) 智能助理查詢服務 *
 * 
 * <pre>
 * 本服務負責協調整個 RAG 流程： 
 * 1. 語意理解：將使用者問題向量化。 
 * 2. 知識檢索：從向量資料庫 (Elasticsearch) 檢索相關上下文 (支援 Tags 精準過濾)。
 * 3. 內容生成：結合上下文與大型語言模型生成精準回答。
 * </pre>
 */
@Slf4j
@Service
public class RagAssistantQueryService {

	private final TextEmbeddingGeneratorPort embeddingGenerator;
	private final KnowledgeVectorSearcherPort vectorSearcher;
	private final ChatClient chatClient;

	/**
	 * 統一的系統提示詞範本
	 */
	private static final String SYSTEM_PROMPT_TEMPLATE = """
			你是一位專業的企業內部知識助理。
			請「嚴格」根據以下 [參考資料] 來回答使用者的問題。
			回答規範：
			1. 如果 [參考資料] 中沒有提及相關資訊，請直接回答「抱歉，目前的知識庫中沒有相關資訊」，絕對不可以捏造事實。
			2. 若能回答，請保持專業、簡潔且條理分明。
			3. 除非使用者要求，否則請優先使用繁體中文回答。

			[參考資料]
			{context}
			""";

	public RagAssistantQueryService(TextEmbeddingGeneratorPort embeddingGenerator,
			KnowledgeVectorSearcherPort vectorSearcher, ChatClient.Builder chatClientBuilder) {
		this.embeddingGenerator = embeddingGenerator;
		this.vectorSearcher = vectorSearcher;
		this.chatClient = chatClientBuilder.build();
	}

	/**
	 * 同步問答模式：執行語意檢索並生成完整回應
	 * 
	 * @param question 使用者的提問
	 * @param tags     預先過濾的標籤條件 (可傳入 null 或空陣列代表全庫搜尋)
	 * @return 包含 AI 回答與參考來源的 DTO
	 */
	public RagResponse askQuestion(String question, List<String> tags) {
		log.info("收到同步問答提問: {}, 標籤過濾: {}", question, tags);

		// 1. 執行檢索並獲取格式化後的上下文
		List<KnowledgeDocumentView> topDocs = retrieveContextDocs(question, tags);

		if (topDocs.isEmpty()) {
			return new RagResponse("抱歉，在指定的範圍內找不到與您問題相關的資料。", List.of());
		}

		String contextText = formatContext(topDocs);

		// 2. 呼叫大語言模型 (LLM) 生成回答
		log.info("🧠 正在呼叫 LLM 執行同步生成...");
		String answer = chatClient.prompt().system(s -> s.text(SYSTEM_PROMPT_TEMPLATE).param("context", contextText))
				.user(question).call().content();

		log.info("✅ 同步回答生成完畢");
		return new RagResponse(answer, topDocs);
	}

	/**
	 * 串流問答模式 (SSE)：執行語意檢索並以 Flux 形式回傳字串片段 * @param question 使用者的提問
	 * 
	 * @param tags 預先過濾的標籤條件 (可傳入 null 或空陣列代表全庫搜尋)
	 * @return 一個會不斷發送字串片段的 Flux 串流
	 */
	public Flux<String> askQuestionStreaming(String question, List<String> tags) {
		log.info("🌊 啟動串流回答模式: {}, 標籤過濾: {}", question, tags);

		// 1. 執行檢索並獲取格式化後的上下文
		List<KnowledgeDocumentView> topDocs = retrieveContextDocs(question, tags);

		if (topDocs.isEmpty()) {
			return Flux.just("抱歉，在指定的範圍內找不到與您問題相關的資料。");
		}

		String contextText = formatContext(topDocs);

		// 2. 呼叫 LLM 並開啟串流模式
		return chatClient.prompt().system(s -> s.text(SYSTEM_PROMPT_TEMPLATE).param("context", contextText))
				.user(question).stream().content();
	}

	/**
	 * 私有輔助方法：根據問題內容與過濾條件檢索最相關的知識文檔
	 */
	private List<KnowledgeDocumentView> retrieveContextDocs(String question, List<String> tags) {
		// 將問題轉化為向量
		float[] queryVector = embeddingGenerator.embed(question);

		// 🌟 KNN 搜尋最相關的 Top 3 筆資料，並帶入 tags 進行 Pre-filtering
		return vectorSearcher.searchSimilar(queryVector, 3, tags);
	}

	/**
	 * 私有輔助方法：將文檔清單轉換為 LLM 可理解的 Context 字串格式
	 */
	private String formatContext(List<KnowledgeDocumentView> docs) {
		return docs.stream().map(doc -> String.format("【%s】%n%s", doc.title(), doc.contentSnippet()))
				.collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
	}

	/**
	 * 內部 DTO：封裝 API 回應
	 */
	public record RagResponse(String answer, List<KnowledgeDocumentView> sources) {
	}
}
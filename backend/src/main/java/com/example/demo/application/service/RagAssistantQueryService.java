package com.example.demo.application.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.example.demo.application.port.KnowledgeVectorSearcherPort;
import com.example.demo.application.port.SemanticCacheManagerPort;
import com.example.demo.application.port.TextEmbeddingGeneratorPort;
import com.example.demo.application.shared.view.KnowledgeDocumentView;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 企業級 RAG (Retrieval-Augmented Generation) 智能助理查詢服務
 * 
 * <pre>
 * 本服務負責協調整個 RAG 流程： 
 * 1. 語意理解：將使用者問題向量化。 
 * 2. 語意快取：🌟 攔截重複的高頻提問，直接回傳歷史解答，降低延遲與成本。
 * 3. 知識檢索：從向量資料庫 (Elasticsearch) 檢索相關上下文 (支援 Tags 精準過濾)。
 * 4. 內容生成：結合上下文與大型語言模型生成精準回答，並於生成後非同步回填快取。
 * </pre>
 */
@Slf4j
@Service
public class RagAssistantQueryService {

	private final TextEmbeddingGeneratorPort embeddingGenerator;
	private final KnowledgeVectorSearcherPort vectorSearcher;
	private final SemanticCacheManagerPort semanticCacheManager; // 🌟 注入快取管理器
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

	public RagAssistantQueryService(
			TextEmbeddingGeneratorPort embeddingGenerator,
			KnowledgeVectorSearcherPort vectorSearcher,
			SemanticCacheManagerPort semanticCacheManager, // 🌟 Constructor 注入
			ChatClient.Builder chatClientBuilder) {
		this.embeddingGenerator = embeddingGenerator;
		this.vectorSearcher = vectorSearcher;
		this.semanticCacheManager = semanticCacheManager;
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

		// 1. 將問題轉化為向量 (供快取與知識檢索共用)
		float[] queryVector = embeddingGenerator.embed(question);

		// 2. 檢查語意快取 (僅在無 Tags 過濾時啟用，避免條件不同卻拿到相同答案)
		boolean canUseCache = (tags == null || tags.isEmpty());
		if (canUseCache) {
			// 設定極高的相似度閾值 0.95，確保語意極度接近才命中
			Optional<String> cachedAnswer = semanticCacheManager.findSimilarAnswer(queryVector, 0.95);
			if (cachedAnswer.isPresent()) {
				log.info("🎯 [同步模式] 命中語意快取！跳過 LLM 生成，直接回傳歷史解答。");
				// 快取命中時，來源文檔傳入空列表，或可特別標記此為快取回應
				return new RagResponse(cachedAnswer.get(), List.of());
			}
		}

		// 3. 快取未命中，執行常規知識庫檢索
		List<KnowledgeDocumentView> topDocs = vectorSearcher.searchSimilar(queryVector, 3, tags);
		if (topDocs.isEmpty()) {
			return new RagResponse("抱歉，在指定的範圍內找不到與您問題相關的資料。", List.of());
		}
		String contextText = formatContext(topDocs);

		// 4. 呼叫大語言模型 (LLM) 生成回答
		log.info("🧠 快取未命中，正在呼叫 LLM 執行同步生成...");
		String answer = chatClient.prompt()
				.system(s -> s.text(SYSTEM_PROMPT_TEMPLATE).param("context", contextText))
				.user(question).call().content();
		log.info("✅ 同步回答生成完畢");

		// 5. 寫入語意快取，造福下一位使用者
		if (canUseCache) {
			semanticCacheManager.putCache(question, queryVector, answer);
		}

		return new RagResponse(answer, topDocs);
	}

	/**
	 * 串流問答模式 (SSE)：執行語意檢索並以 Flux 形式回傳字串片段
	 * 
	 * @param question 使用者的提問
	 * @param tags 預先過濾的標籤條件 (可傳入 null 或空陣列代表全庫搜尋)
	 * @return 一個會不斷發送字串片段的 Flux 串流
	 */
	public Flux<String> askQuestionStreaming(String question, List<String> tags) {
		log.info("🌊 啟動串流回答模式: {}, 標籤過濾: {}", question, tags);

		// 1. 將問題轉化為向量
		float[] queryVector = embeddingGenerator.embed(question);

		// 2. 🌟 檢查語意快取
		boolean canUseCache = (tags == null || tags.isEmpty());
		if (canUseCache) {
			Optional<String> cachedAnswer = semanticCacheManager.findSimilarAnswer(queryVector, 0.95);
			if (cachedAnswer.isPresent()) {
				log.info("🎯 [串流模式] 命中語意快取！將歷史解答包裝為 Flux 發送。");
				return Flux.just(cachedAnswer.get());
			}
		}

		// 3. 執行檢索並獲取格式化後的上下文
		List<KnowledgeDocumentView> topDocs = vectorSearcher.searchSimilar(queryVector, 3, tags);
		if (topDocs.isEmpty()) {
			return Flux.just("抱歉，在指定的範圍內找不到與您問題相關的資料。");
		}
		String contextText = formatContext(topDocs);

		log.info("🧠 快取未命中，呼叫 LLM 開啟串流模式...");

		// 🌟 準備一個 StringBuilder 用來攔截並組合串流文字
		StringBuilder fullAnswerBuilder = new StringBuilder();

		// 4. 呼叫 LLM 並開啟串流模式，同時掛載生命週期監聽
		return chatClient.prompt()
				.system(s -> s.text(SYSTEM_PROMPT_TEMPLATE).param("context", contextText))
				.user(question)
				.stream().content()
				.doOnNext(fullAnswerBuilder::append) // 每收到一個字串片段，就存進 StringBuilder
				.doOnComplete(() -> {
					// 5. 🌟 當串流順利結束時，將完整拼湊好的答案寫入快取
					if (canUseCache) {
						String finalAnswer = fullAnswerBuilder.toString();
						log.debug("✅ 串流生成完畢，準備回填語意快取 (長度: {})", finalAnswer.length());
						semanticCacheManager.putCache(question, queryVector, finalAnswer);
					}
				});
	}

	/**
	 * 私有輔助方法：將文檔清單轉換為 LLM 可理解的 Context 字串格式
	 */
	private String formatContext(List<KnowledgeDocumentView> docs) {
		return docs.stream()
				.map(doc -> String.format("【%s】%n%s", doc.title(), doc.contentSnippet()))
				.collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
	}

	/**
	 * 內部 DTO：封裝 API 回應
	 */
	public record RagResponse(String answer, List<KnowledgeDocumentView> sources) {
	}
}
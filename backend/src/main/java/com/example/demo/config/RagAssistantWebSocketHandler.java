package com.example.demo.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
//🌟 下面這四個 Import 是成敗的關鍵，絕對不能出現 reactive 字眼！
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.demo.application.service.RagAssistantQueryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 企業大腦 AI 助理 WebSocket 處理器
 * <p>
 * 負責處理前端與 AI 助理之間的雙向即時通訊。 接收包含問題與檢索標籤的 JSON 請求，並透過訂閱 Reactive 串流 (Flux)， 將 AI
 * 產生的文字片段 (Chunks) 即時推播回前端，實現打字機效果。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagAssistantWebSocketHandler extends TextWebSocketHandler {

	/**
	 * 處理 RAG 檢索與呼叫本地/遠端 LLM 的核心服務
	 */
	private final RagAssistantQueryService ragService;

	/**
	 * 用於解析前端傳來的 JSON 字串
	 */
	private final ObjectMapper objectMapper;

	/**
	 * 當 WebSocket 連線成功建立時觸發
	 *
	 * @param session 當前的 WebSocket 連線實體
	 * @throws Exception 若處理過程中發生例外
	 */
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		log.info("[WebSocket] 成功建立新連線 | Session ID: {}", session.getId());
	}

	/**
	 * 處理接收到的文字訊息（前端傳來的提問）
	 *
	 * @param session 當前的 WebSocket 連線實體
	 * @param message 包含 JSON Payload 的文字訊息
	 * @throws Exception 若處理過程中發生例外
	 */
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		try {
			// 1. 取得並解析前端傳來的 JSON 字串
			String payload = message.getPayload();
			JsonNode root = objectMapper.readTree(payload);

			// 2. 提取使用者問題，若無則預設為空字串
			String question = root.path("question").asText("");

			// 3. 提取過濾標籤陣列
			List<String> tags = new ArrayList<>();
			if (root.path("tags").isArray()) {
				root.path("tags").forEach(node -> tags.add(node.asText()));
			}

			// 4. 防呆檢查：確保問題不是空白
			if (question.isBlank()) {
				session.sendMessage(new TextMessage("{\"error\": \"問題不可為空\"}"));
				return;
			}

			log.info("[WebSocket] 收到提問: {}", question);

			// 5. 呼叫 AI 服務並訂閱串流 (Subscribe)
			ragService.askQuestionStreaming(question, tags).subscribe(
					// 階段 A (onNext): 處理接收到的每一個字元片段 (Chunk)
					chunk -> {
						try {
							// 確保連線還活著才發送，避免 Broken pipe 錯誤
							if (session.isOpen()) {
								session.sendMessage(new TextMessage(chunk));
							}
						} catch (Exception e) {
							log.error("[WebSocket] 推播訊息片段失敗", e);
						}
					},
					// 階段 B (onError): 處理 AI 生成或檢索過程中的錯誤
					error -> {
						log.error("[WebSocket] AI 生成異常", error);
						try {
							if (session.isOpen()) {
								// 傳送標準化的錯誤 JSON 給前端，讓前端可以觸發 Toast 提示
								session.sendMessage(new TextMessage("{\"error\": \"生成回覆時發生異常\"}"));
							}
						} catch (Exception ignored) {
							// 若連發送錯誤訊息都失敗，通常代表連線已斷開，此處忽略例外
						}
					},
					// 階段 C (onComplete): 串流正常結束時觸發
					() -> {
						try {
							if (session.isOpen()) {
								// 傳送與前端約定好的結束暗號，讓前端解鎖輸入框
								session.sendMessage(new TextMessage("[DONE]"));
							}
						} catch (Exception e) {
							log.error("[WebSocket] 傳送結束標記失敗", e);
						}
					});

		} catch (Exception e) {
			log.error("[WebSocket] 訊息解析失敗，Payload 可能不是合法的 JSON", e);
		}
	}

	/**
	 * 當 WebSocket 連線關閉或中斷時觸發
	 *
	 * @param session 當前的 WebSocket 連線實體
	 * @param status  關閉狀態碼與原因
	 * @throws Exception 若處理過程中發生例外
	 */
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		log.info("[WebSocket] 連線已中斷 | Session ID: {}", session.getId());
	}
}
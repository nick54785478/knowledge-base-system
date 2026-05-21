package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import lombok.RequiredArgsConstructor;

/**
 * 企業大腦 AI 助理 WebSocket 核心配置類別
 * <p>
 * 負責啟動 Spring MVC 的 WebSocket 支援，並註冊自定義的 WebSocket 處理器 (Handler)。 此類別集中管理了 AI
 * 助理相關的即時通訊路由，以及跨來源資源共用 (CORS) 的安全設定。
 * </p>
 */
@Configuration
@EnableWebSocket // 啟動 Spring MVC 的 WebSocket 支援
@RequiredArgsConstructor
public class WebSocketConfiguration implements WebSocketConfigurer {

	/**
	 * 注入自定義的 AI 對話處理器 負責處理實際的連線建立、訊息接收 (提問) 與串流推播 (AI 回覆) 邏輯
	 */
	private final RagAssistantWebSocketHandler ragWebSocketHandler;

	/**
	 * 註冊 WebSocket 處理器與對應的連線端點 (Endpoint)
	 *
	 * @param registry WebSocket 處理器註冊表，用於配置路由與跨域策略
	 */
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

		// 1. 註冊路由：將 "/api/v1/assistant/ws" 這個 URL 交給 ragWebSocketHandler 處理
		// 2. 開放跨域：setAllowedOrigins("*") 允許來自任何網域的 WebSocket 升級請求 (Upgrade)
		// 注意：在正式上線 (Production) 環境中，建議將 "*" 替換為實際的前端網域 (例如
		// "https://virgo.internal.com") 以提升安全性。
		registry.addHandler(ragWebSocketHandler, "/api/v1/assistant/ws").setAllowedOrigins("*");
	}
}
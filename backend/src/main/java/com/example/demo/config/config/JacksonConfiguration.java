package com.example.demo.config.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 企業級 Jackson JSON 序列化核心配置類別
 * <p>
 * 此類別負責全域性地自定義 Spring Boot 內建的 ObjectMapper 實例。
 * 透過調整序列化與反序列化的策略，提升系統對於各種資料格式（如時間、複雜事件 Payload）的相容性。
 * </p>
 */
@Configuration
public class JacksonConfiguration {

	/**
	 * 自定義並註冊全域的 ObjectMapper Bean
	 * <pre>
	 * 1. 註冊 {@link JavaTimeModule}： 讓 Jackson 能夠正確解析與輸出 Java 8 的新版日期時間 API (例如
	 * Instant, LocalDateTime)， 解決預設情況下時間物件被轉換成詭異的 Array 格式的問題。 <br>
	 * 2. 停用 {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}：
	 * 【實戰關鍵】當系統接收到外部系統 (如 Kafka, Debezium) 傳來的龐大 JSON Payload 時， 外部 JSON
	 * 中通常會帶有大量我們不需要的 Metadata (例如 Debezium 的 source, transaction 等欄位)。 將此屬性設為
	 * false，可確保系統在反序列化時，只要專注讀取我們定義在 DTO 裡的欄位（如 before, after）即可， 遇到未知的欄位會自動忽略而不會拋出
	 * Exception 導致系統崩潰。
	 * </pre>
	 *
	 * @return 配置完成的 ObjectMapper 實例
	 */
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();

		// 支援 JDK 8 的新版時間 API (Instant, LocalDateTime 等)
		mapper.registerModule(new JavaTimeModule());

		// 寬容處理機制：遇到不認識的 JSON 欄位時忽略，不要拋出例外
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		return mapper;
	}
}
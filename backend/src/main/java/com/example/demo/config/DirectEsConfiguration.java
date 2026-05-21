package com.example.demo.config;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

/**
 * 自訂 Elasticsearch 客戶端底層配置。
 * 
 * <p>
 * 本配置類別刻意棄用 Spring Boot 的自動配置 (Auto-configuration)， 改為手動建構 Elasticsearch
 * 的連線三部曲：RestClient -> Transport -> ElasticsearchClient。
 * </p>
 * 
 * <pre>
 * 在正常情況下，Spring Boot 會透過 spring.elasticsearch.* 
 * 設定檔自動幫你做完這三步。但我們為了除錯，選擇了「手排模式」。
 * 1. 突破框架黑盒子：Spring Boot 封裝得太好，導致我們無法介入底層。這段程式碼直接退回使用 Elasticsearch 官方的 Java API，讓我們拿回對 HTTP 請求的絕對控制權。
 * 2. 攔截與竄改 (降級打擊)：這段配置最核心的價值在於那個 Interceptor。因為我們的 Java 環境套件較新 (準備打給 ES 9.x)，但伺服器較舊 (ES 8.x)。
 * 這段程式碼做的事情，就像是把寫著「未來語言」的信封撕掉，換成伺服器看得懂的「標準通用信封 (application/json)」，從而繞過了 ES 嚴格的版本相容性檢查。
 * </pre>
 * <p>
 * <b>核心目的：</b><br>
 * 解決因客戶端版本超前 (如 9.x 連線至 8.x) 或本地端防毒軟體/Proxy 竄改 HTTP Headers， 導致 ES 伺服器拋出
 * {@code [media_type_header_exception]} 400 錯誤的問題。 透過在 Apache HTTP
 * 底層註冊攔截器，強制將特殊的 MIME Type 洗白為標準的 {@code application/json}。
 * </p>
 *
 */
@Configuration
public class DirectEsConfiguration {

	/**
	 * 1. 建立最底層的 HTTP 通訊客戶端 (Apache HttpAsyncClient)
	 * <p>
	 * 這裡是發送 HTTP 請求的最後一關。我們在此處掛載了一個 {@link HttpRequestInterceptor}， 在封包即將離開 JVM
	 * 前，強制攔截並修改 Headers。
	 * </p>
	 * 
	 * @return 基礎的 RestClient
	 */
	@Bean
	public RestClient restClient() {
		return RestClient.builder(new HttpHost("127.0.0.1", 19200)).setHttpClientConfigCallback(httpClientBuilder -> {

			/*
			 * 掛上 Apache 原生的攔截器 (Interceptor) 執行時機：封包已經組裝完畢，準備透過 TCP 送出前的最後一刻。
			 */
			httpClientBuilder.addInterceptorLast((HttpRequestInterceptor) (request, context) -> {

				// [警告] 以下 System.out 僅供本地端 Debug 使用，正式環境應改用 log.trace 或移除
				System.out.println("### [底層攔截] 封包離開 JVM 前的真實 Headers ###");
				for (Header header : request.getAllHeaders()) {
					System.out.println("   " + header.getName() + " : " + header.getValue());
				}
				System.out.println("=================================================");

				/*
				 * 強制拔除所有被污染或版本不相容的標頭。 例如 Spring Data 預設會帶上的：
				 * application/vnd.elasticsearch+json; compatible-with=9
				 */
				request.removeHeaders("Accept");
				request.removeHeaders("Content-Type");

				/*
				 * 寫入最乾淨、最通用的 JSON 標頭。 藉此騙過防毒軟體的深度封包檢測 (DPI)，並相容於舊版 ES 伺服器。
				 */
				request.addHeader("Accept", "application/json");
				request.addHeader("Content-Type", "application/json");
			});

			return httpClientBuilder;
		}).build();
	}

	/**
	 * 2. 建立資料傳輸層 (Transport Layer)
	 * <p>
	 * 負責將高階的 Java 物件與底層的 JSON 字串進行轉換。 這裡明確指定使用 Spring Boot 生態中最穩定的 Jackson 作為 JSON
	 * 解析器。
	 * </p>
	 * 
	 * @param restClient 底層通訊客戶端
	 * @return 具備物件綁定能力的 ElasticsearchTransport
	 */
	@Bean
	public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
		return new RestClientTransport(restClient, new JacksonJsonpMapper());
	}

	/**
	 * 3. 建立高階客戶端 (High-level Client)
	 * <p>
	 * 只要將這個 Bean 註冊進 Spring 容器，Spring Data Elasticsearch 底層的 Repository (如
	 * ArticleRepository) 就會自動取得並使用它來執行所有資料庫操作。
	 * </p>
	 * 
	 * @param transport 資料傳輸層
	 * @return 供業務邏輯與 Repository 使用的高階客戶端
	 */
	@Bean
	public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
		return new ElasticsearchClient(transport);
	}
}
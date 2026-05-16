# Enterprise RAG Knowledge Base Backend (企業級智能知識庫後端)

這是一個基於 六角形架構 (Hexagonal Architecture) 與 CQRS (命令查詢職責分離) 模式打造的企業級智能知識庫與 RAG (Retrieval-Augmented Generation) 助理系統，本專案不僅提供基礎的 CRUD 功能，更結合了 CDC (變更資料擷取) 技術與 本地端大語言模型 (Local LLM)，實現了從關聯式資料庫到向量資料庫的「自動化語意擴充」與「即時串流問答」。

## 系統架構亮點

* **六角形架構 (Ports and Adapters)**：

> 核心業務邏輯被嚴格保護在 Domain 與 Application 領域，與外部基礎建設（如資料庫、AI 服務、Message Broker）完全解耦。

* **CQRS 讀寫分離**：

>* Write Model：以 PostgreSQL 作為 Single Source of Truth，確保資料的 ACID 特性。
>* Read Model：以 Elasticsearch 9.x 作為檢索端，支援高效能的 KNN 向量相似度搜尋。

* **Event-Driven CDC 雙寫機制**：

> 透過 Debezium 監聽 PostgreSQL 的 WAL，並經由 Kafka 派發事件。Logstash 負責基礎文本同步，而 Java 後端負責呼叫 AI 進行向量計算與 Partial Update。

* **優雅重試與容錯 (Graceful Retry)**：

> 針對分散式系統中常見的 Race Condition (如 Logstash 寫入延遲導致 ES 找不到文檔)，實作了具備 Exponential Backoff 思維的重試機制，確保資料最終一致性。


## 技術堆疊 (Tech Stack)

* **核心框架**：Java 21, Spring Boot 4.0.6, Spring WebFlux

* **AI 整合**：Spring AI (2.0.0-M3)

* **本地大模型**：Ollama (Qwen 2.5 7B 用於生成, Nomic-Embed-Text 用於向量化)

* **資料持久層**：PostgreSQL, Spring Data JPA

* **向量搜尋引擎**：Elasticsearch 9.2.8 (Java API Client)

* **訊息中介軟體與 CDC**：Apache Kafka, Debezium, Logstash

## 核心功能 (Core Features)

* **非同步 AI 向量化 (AI Enrichment)**:

> 當使用者新增/修改知識文檔時，系統會攔截 Kafka 事件，自動在背景呼叫 Embedding 模型，並將 768 維的向量特徵補齊至 Elasticsearch 中。

* **高精準度語意搜尋 (Semantic Search)**:

> 利用 Elasticsearch 的 KNN 演算法，支援 numCandidates 擴展與 Source Filtering，以極低的網路 I/O 成本提供極高的檢索召回率。

* **RAG 智能問答 (防幻覺機制)**:

> 結合 System Prompt 嚴格限制 AI 僅能根據企業知識庫的內容回答。若無相關資料，系統會優雅地回覆無法回答，拒絕捏造事實。

* **SSE 串流輸出 (Server-Sent Events)**:

> 實作 Flux 非同步資料流，讓 AI 生成的字元能像「打字機」一般即時推播至前端，提供零等待的順暢使用者體驗 (Time to First Token < 1s)。

## 快速啟動 (Getting Started)

**1. 啟動基礎建設 (Docker)**

執行以下指令啟動所有相依服務：

	docker-compose up -d

啟動包含：

>* PostgreSQL
>* Kafka 
>* Elasticsearch 
>* Debezium
>* Logstash
>* Kibana 
>* Ollama

**2. 準備 AI 模型**

進入 Ollama 容器並下載所需的模型：

	docker exec -it kb_ollama ollama pull qwen2.5:7b
	docker exec -it kb_ollama ollama pull nomic-embed-text:latest
	
**3. 初始化 Elasticsearch 向量索引**

因 ES 9.x 版本差異，請進入 Kibana Dev Tools (http://localhost:5601) 執行以下 DDL 建立 Index：

	PUT /knowledge_vector
	{
	  "mappings": {
	    "properties": {
	      "embedding_vector": { "type": "dense_vector", "dims": 768, "index": true, "similarity": "cosine" },
	      "id": { "type": "keyword" },
	      "title": { "type": "text" },
	      "content": { "type": "text" },
	      "category": { "type": "keyword" },
	      "author": { "type": "keyword" }
	    }
	  }
	}

**4. 啟動 Spring Boot 應用程式**

可測試以下 API : 

* 新增文檔 (觸發 CDC 與向量化)

	POST /api/v1/documents
	Content-Type: application/json
	
	{
	  "title": "海外出差補助與報帳規範",
	  "content": "亞洲地區每日補助新台幣3000元，歐美地區每日5000元...",
	  "author": "HR",
	  "category": "財務規範"
	}
	
* RAG 串流問答 (請透過瀏覽器或支援 SSE 的客戶端)

	GET http://localhost:8080/api/v1/assistant/ask-stream?q=下週去德國參展五天，可以補助多少？
	

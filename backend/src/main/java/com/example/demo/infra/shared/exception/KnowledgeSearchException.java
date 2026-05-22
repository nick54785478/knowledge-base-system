package com.example.demo.infra.shared.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知識庫檢索專用例外 (Custom Exception)
 * <p>
 * 用於封裝底層搜尋引擎 (如 Elasticsearch, Vector Database) 的實作細節。 讓應用層 (Application
 * Service) 能夠透過捕捉此例外來進行重試或降級處理， 而不必依賴特定的第三方函式庫例外。
 * </p>
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KnowledgeSearchException extends RuntimeException {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public KnowledgeSearchException(String message) {
        super(message);
    }

    public KnowledgeSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
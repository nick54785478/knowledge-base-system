package com.example.demo.application.port;

/**
 * 系統分類字典 Port
 */
public interface SystemDictionaryPort {

	/**
	 * 根據代碼取得分類中文名稱 (建議底層實作加上 @Cacheable 快取)
	 * 
	 * @param code 分類代碼
	 * @return 分類中文名
	 */
	String getCategoryNameByCode(String code);
}

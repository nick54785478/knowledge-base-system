package com.example.demo.infra.es.constant;


/**
 * 知識庫 Elasticsearch 索引欄位與配置常數池
 */
public final class KnowledgeIndexConstants {

    private KnowledgeIndexConstants() {
        // 防止實例化
    }

    // --- 欄位名稱 (Fields) ---
    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_CONTENT = "content";
    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_CATEGORY_NAME = "categoryName";
    public static final String FIELD_TAGS = "tags";
    public static final String FIELD_AUTHOR = "author";

    // --- 查詢權重與特定後綴 ---
    public static final String FIELD_TITLE_BOOST = FIELD_TITLE + "^3";
    public static final String FIELD_TAGS_KEYWORD = FIELD_TAGS + ".keyword";

    // --- 高亮配置 (Highlighting) ---
    public static final String HIGHLIGHT_PRE_TAG = "<em class='highlight'>";
    public static final String HIGHLIGHT_POST_TAG = "</em>";

    // --- 聚合名稱 (Aggregations) ---
    public static final String AGG_POPULAR_TAGS = "popular_tags";
    
    // --- 預設值 ---
    public static final String DEFAULT_UNKNOWN = "未知";
}
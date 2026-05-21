-- 建立實體資料表
CREATE TABLE knowledge_document (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    author VARCHAR(100),
    category VARCHAR(50),
    status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 關鍵：設定 WAL (Write-Ahead Logging) 的層級
-- 這行是告訴 Postgres：「當這張表有任何異動時，請把舊資料跟新資料都寫進日誌裡，讓 CDC 抓得到」
ALTER TABLE knowledge_document REPLICA IDENTITY DEFAULT;


-- 建立稽核軌跡主表
CREATE TABLE knowledge_document_audit_log (
    id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    operation_type VARCHAR(10),
    before_state TEXT,
    after_state TEXT,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE knowledge_document_audit_log (
    id VARCHAR(36) NOT NULL,
    document_id VARCHAR(36) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    before_state TEXT,
    after_state TEXT,
    changed_at TIMESTAMP NOT NULL,
    -- 🚨 關鍵：複合主鍵必須包含分區鍵
    PRIMARY KEY (id, changed_at) 
) PARTITION BY RANGE (changed_at);

-- 2. 建立當前月份與下個月的分區表 (子表)
CREATE TABLE audit_log_2026_05 PARTITION OF knowledge_document_audit_log
    FOR VALUES FROM ('2026-05-01 00:00:00') TO ('2026-06-01 00:00:00');

CREATE TABLE audit_log_2026_06 PARTITION OF knowledge_document_audit_log
    FOR VALUES FROM ('2026-06-01 00:00:00') TO ('2026-07-01 00:00:00');
    
CREATE TABLE setting (
    id BIGSERIAL,
    data_type VARCHAR(20),
    type VARCHAR(100),
    name VARCHAR(100),
    code VARCHAR(100),
    value VARCHAR(255),
    description TEXT,
    priority_no INT,
    active_flag VARCHAR(1) DEFAULT 'Y',
    PRIMARY KEY (id)
);
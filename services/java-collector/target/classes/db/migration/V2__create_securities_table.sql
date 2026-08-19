-- Создание таблицы инструментов
CREATE TABLE IF NOT EXISTS securities (
    security_id VARCHAR(20) PRIMARY KEY,
    security_name VARCHAR(100),
    short_name VARCHAR(50),
    lot_size INT DEFAULT 1,
    is_traded BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Индекс для поиска по имени
CREATE INDEX IF NOT EXISTS idx_securities_name 
ON securities (security_name);

-- Создание таблицы истории торгов
CREATE TABLE IF NOT EXISTS trade_history (
    id BIGSERIAL PRIMARY KEY,
    security_id VARCHAR(20) NOT NULL,
    trade_time TIMESTAMPTZ NOT NULL,
    price DECIMAL(20, 6),
    quantity INT,
    volume DECIMAL(20, 6),
    trade_type VARCHAR(10),
    FOREIGN KEY (security_id) REFERENCES securities(security_id)
);

-- Индекс для быстрого поиска
CREATE INDEX IF NOT EXISTS idx_trade_history_security_time 
ON trade_history (security_id, trade_time DESC);

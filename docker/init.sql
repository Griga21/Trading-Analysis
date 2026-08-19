CREATE TABLE IF NOT EXISTS candles (
    id BIGSERIAL PRIMARY KEY,
    security_id VARCHAR(20) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    open DECIMAL(20, 6),
    high DECIMAL(20, 6),
    low DECIMAL(20, 6),
    close DECIMAL(20, 6),
    volume DECIMAL(20, 6),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_candle UNIQUE (security_id, timestamp)
);

CREATE INDEX IF NOT EXISTS idx_candles_security_time 
ON candles (security_id, timestamp DESC);
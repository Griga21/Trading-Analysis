CREATE TABLE security_indicators (
    security_id VARCHAR NOT NULL,
    date DATE,
    sma_20 FLOAT,
    sma_50 FLOAT,
    ema_20 FLOAT,
    volatility_20 FLOAT,
    rsi_14 FLOAT,
    PRIMARY KEY (security_id, date)
);

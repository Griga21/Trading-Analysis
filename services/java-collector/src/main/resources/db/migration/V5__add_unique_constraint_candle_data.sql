ALTER TABLE candles
ADD CONSTRAINT unique_candle_data UNIQUE (security_id, timestamp);
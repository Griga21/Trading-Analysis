CREATE TABLE securities (
    security_id VARCHAR(20) PRIMARY KEY,
    short_name VARCHAR(255),
    board VARCHAR(20) NOT NULL DEFAULT 'TQBR',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_collected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
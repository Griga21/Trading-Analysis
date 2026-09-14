CREATE TABLE securities (
    security_id VARCHAR(20) PRIMARY KEY,
    short_name VARCHAR(255),
    board VARCHAR(20) NOT NULL DEFAULT 'TQBR',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_collected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);


INSERT INTO securities (security_id, short_name, board, is_active, created_at)
VALUES
    ('SBER', 'Сбербанк', 'TQBR', true, now()),
    ('GAZP', 'Газпром',  'TQBR', true, now()),
    ('LKOH', 'Лукойл',   'TQBR', true, now()),
    ('MTSS', 'МТС',      'TQBR', true, now())
ON CONFLICT (security_id) DO NOTHING;
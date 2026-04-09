CREATE TABLE games (
    id               BIGINT PRIMARY KEY,
    name             TEXT NOT NULL,
    year_published   INT,
    bgg_rank         INT,
    avg_rating       NUMERIC(4,2),
    complexity       NUMERIC(3,2),
    min_players      INT,
    max_players      INT,
    thumbnail_url    TEXT,
    description      TEXT,
    play_time_min    INT,
    play_time_max    INT,
    last_synced_at   TIMESTAMP NOT NULL
);

CREATE INDEX idx_games_bgg_rank   ON games (bgg_rank);
CREATE INDEX idx_games_avg_rating ON games (avg_rating);
CREATE INDEX idx_games_complexity ON games (complexity);
CREATE INDEX idx_games_name       ON games (name);

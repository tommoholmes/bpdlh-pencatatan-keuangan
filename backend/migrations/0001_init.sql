CREATE TABLE users (
  id         TEXT PRIMARY KEY,
  created_at INTEGER NOT NULL,
  seq        INTEGER NOT NULL DEFAULT 0,
  log_id     TEXT    NOT NULL
);

CREATE TABLE records (
  user_id      TEXT    NOT NULL,
  id           TEXT    NOT NULL,
  name         TEXT    NOT NULL,
  price        INTEGER NOT NULL CHECK (price >= 0),
  photo_id     TEXT,
  latitude     REAL CHECK (latitude  BETWEEN -90  AND 90),
  longitude    REAL CHECK (longitude BETWEEN -180 AND 180),
  accuracy_m   REAL,
  device_id    TEXT,
  device_model TEXT,
  os_version   TEXT,
  app_version  TEXT,
  created_at   INTEGER NOT NULL,
  updated_at   INTEGER NOT NULL,
  deleted_at   INTEGER,
  seq          INTEGER NOT NULL,
  PRIMARY KEY (user_id, id),
  CHECK ((latitude IS NULL) = (longitude IS NULL))
);

CREATE INDEX idx_records_seq ON records (user_id, seq);

-- HSDC persistence schema (ADR-004 §4).
-- Idempotent by design: run on every application start
-- (spring.sql.init.mode=always). All timestamps are ISO-8601 TEXT, UTC.
-- foreign_keys enforcement is turned on at the driver/connection level
-- (see DataSourceConfig), not here.

CREATE TABLE IF NOT EXISTS customer (
    id    TEXT PRIMARY KEY,
    name  TEXT NOT NULL,
    email TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS purchase (
    id            TEXT PRIMARY KEY,
    customer_id   TEXT NOT NULL REFERENCES customer (id),
    order_number  TEXT NOT NULL UNIQUE,
    product_name  TEXT NOT NULL,
    category      TEXT NOT NULL,
    purchase_date TEXT NOT NULL,
    price_cents   INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS case_session (
    id                  TEXT PRIMARY KEY,
    request_type        TEXT NOT NULL CHECK (request_type IN ('COMPLAINT', 'RETURN')),
    equipment_category  TEXT NOT NULL,
    equipment_model     TEXT NOT NULL,
    purchase_date       TEXT NOT NULL,
    order_number        TEXT,
    reason              TEXT,
    image_analysis      TEXT,
    order_verified      INTEGER CHECK (order_verified IN (0, 1)),
    created_at          TEXT NOT NULL
);
-- Deliberately no FK from case_session.order_number to purchase.order_number:
-- unverified orders must remain storable (AC-15).

CREATE TABLE IF NOT EXISTS decision (
    id             TEXT PRIMARY KEY,
    case_id        TEXT NOT NULL REFERENCES case_session (id),
    category       TEXT NOT NULL CHECK (category IN ('APPROVE', 'REJECT', 'NEEDS_MORE_INFO')),
    justification  TEXT NOT NULL,
    full_message   TEXT NOT NULL,
    created_at     TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_decision_case_created ON decision (case_id, created_at);

CREATE TABLE IF NOT EXISTS chat_message (
    id         TEXT PRIMARY KEY,
    case_id    TEXT NOT NULL REFERENCES case_session (id),
    sender     TEXT NOT NULL CHECK (sender IN ('CUSTOMER', 'AGENT')),
    content    TEXT NOT NULL,
    created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_chat_message_case_created ON chat_message (case_id, created_at);

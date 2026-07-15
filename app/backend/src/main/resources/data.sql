-- HSDC seed data (ADR-004 §4, §8 TAC-004-03).
-- Idempotent: fixed IDs + INSERT OR IGNORE, safe to run on every start.
--
-- All purchase-date boundary categories below are computed against a fixed
-- reference "today" of 2026-07-15 (see SeedDataBoundaryTest), which keeps
-- return-window (14 days) and warranty (24 months) boundary tests
-- deterministic regardless of when the test suite actually runs.

INSERT OR IGNORE INTO customer (id, name, email) VALUES
    ('CUST-001', 'John Kowalski', 'john.kowalski@example.com'),
    ('CUST-002', 'Anna Nowak', 'anna.nowak@example.com'),
    ('CUST-003', 'Marek Wisniewski', 'marek.wisniewski@example.com'),
    ('CUST-004', 'Ewa Zielinska', 'ewa.zielinska@example.com'),
    ('CUST-005', 'Tomasz Lewandowski', 'tomasz.lewandowski@example.com');

-- Order number | purchase_date | boundary category (relative to 2026-07-15)
-- ORD-1001     | 2026-07-10    | within 14-day return window (5 days ago); within 24-mo warranty
-- ORD-1002     | 2026-05-01    | outside 14-day return window (75 days ago); within 24-mo warranty
-- ORD-1003     | 2022-01-15    | outside 14-day return window; outside 24-mo warranty
-- ORD-1004     | 2026-07-01    | exactly 14 days ago -> return-window boundary (inclusive edge)
-- ORD-1005     | 2024-07-15    | exactly 24 months ago -> warranty boundary (inclusive edge)
-- ORD-1006     | 2026-06-30    | 15 days ago -> just outside the 14-day return window
-- ORD-1007     | 2024-07-14    | 24 months + 1 day ago -> just outside the 24-mo warranty
-- ORD-1008     | 2025-11-20    | date-mismatch demo case: submit a different purchase_date on
--                                the form than this stored date to exercise the agent's cross-check
-- ORD-1009     | 2026-07-13    | 2 days ago -> well within the 14-day return window
-- ORD-1010     | 2020-03-01    | far outside both the return window and the warranty
INSERT OR IGNORE INTO purchase (id, customer_id, order_number, product_name, category, purchase_date, price_cents) VALUES
    ('PUR-1001', 'CUST-001', 'ORD-1001', 'Robot Vacuum X200',  'Home Appliances', '2026-07-10', 129900),
    ('PUR-1002', 'CUST-002', 'ORD-1002', '4K Monitor 27in',    'Electronics',     '2026-05-01',  89900),
    ('PUR-1003', 'CUST-003', 'ORD-1003', 'Electric Kettle',    'Home Appliances', '2022-01-15',   4900),
    ('PUR-1004', 'CUST-004', 'ORD-1004', 'Wireless Mouse',     'Electronics',     '2026-07-01',   9900),
    ('PUR-1005', 'CUST-005', 'ORD-1005', 'Espresso Machine',   'Home Appliances', '2024-07-15', 249900),
    ('PUR-1006', 'CUST-001', 'ORD-1006', 'Gaming Keyboard',    'Electronics',     '2026-06-30',  15900),
    ('PUR-1007', 'CUST-002', 'ORD-1007', 'Blender Pro',        'Home Appliances', '2024-07-14',   7900),
    ('PUR-1008', 'CUST-003', 'ORD-1008', 'Bluetooth Speaker',  'Electronics',     '2025-11-20',  24900),
    ('PUR-1009', 'CUST-004', 'ORD-1009', 'Air Fryer',          'Home Appliances', '2026-07-13',  17900),
    ('PUR-1010', 'CUST-005', 'ORD-1010', 'Old Toaster',        'Home Appliances', '2020-03-01',   3900);

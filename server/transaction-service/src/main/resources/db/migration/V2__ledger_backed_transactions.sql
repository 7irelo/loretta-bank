-- Money now lives in account-service's double-entry ledger. This service keeps the customer
-- facing transaction history and the lifecycle of each instruction (PENDING -> COMPLETED/FAILED).

DROP TABLE transfer_sagas;

ALTER TABLE transactions
    ADD COLUMN source_customer_id BIGINT,
    ADD COLUMN target_customer_id BIGINT,
    ADD COLUMN initiated_by VARCHAR(64),
    ADD COLUMN failure_reason TEXT,
    ADD COLUMN journal_id UUID,
    ADD COLUMN attempts INT NOT NULL DEFAULT 0,
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN status SET DEFAULT 'PENDING';

ALTER TABLE ledger_entries
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE outbox_events
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

CREATE INDEX idx_transactions_pending ON transactions(status, updated_at) WHERE status = 'PENDING';
CREATE INDEX idx_transactions_source_customer ON transactions(source_customer_id, created_at);
CREATE INDEX idx_ledger_account_created ON ledger_entries(account_id, created_at DESC);

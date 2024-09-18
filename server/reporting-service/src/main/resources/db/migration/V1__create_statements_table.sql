CREATE TABLE statements (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'ZAR',
    period_from TIMESTAMP NOT NULL,
    period_to TIMESTAMP NOT NULL,
    opening_balance NUMERIC(19,4) NOT NULL,
    closing_balance NUMERIC(19,4) NOT NULL,
    total_credits NUMERIC(19,4) NOT NULL,
    total_debits NUMERIC(19,4) NOT NULL,
    transaction_count INTEGER NOT NULL,
    line_items_json JSONB NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_statements_account_id ON statements(account_id);
CREATE INDEX idx_statements_customer_id ON statements(customer_id);
CREATE INDEX idx_statements_generated_at ON statements(generated_at DESC);

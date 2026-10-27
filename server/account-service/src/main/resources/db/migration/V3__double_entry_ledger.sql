-- Account-service is the single owner of money. Balances are a projection of immutable,
-- double-entry postings that are written in the same transaction as the balance update.

ALTER TABLE accounts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE accounts ADD CONSTRAINT chk_accounts_overdraft_limit CHECK (overdraft_limit >= 0);

CREATE TABLE journals (
    id              UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    journal_type    VARCHAR(40)  NOT NULL,
    reference       VARCHAR(64)  NOT NULL,
    description     VARCHAR(255),
    currency        VARCHAR(3)   NOT NULL,
    created_by      VARCHAR(64)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- A posting hits exactly one customer account or one internal system account.
-- amount is signed: positive credits the account, negative debits it.
CREATE TABLE postings (
    id             BIGSERIAL PRIMARY KEY,
    journal_id     UUID          NOT NULL REFERENCES journals(id),
    account_id     BIGINT        REFERENCES accounts(id),
    system_account VARCHAR(40),
    amount         NUMERIC(19,4) NOT NULL CHECK (amount <> 0),
    balance_after  NUMERIC(19,4),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_postings_target CHECK ((account_id IS NULL) <> (system_account IS NULL))
);

CREATE INDEX idx_postings_journal ON postings(journal_id);
CREATE INDEX idx_postings_account_id ON postings(account_id, id);

-- Postings are append-only: corrections are made with reversing journals.
CREATE FUNCTION forbid_posting_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'postings are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_postings_immutable
    BEFORE UPDATE OR DELETE ON postings
    FOR EACH ROW EXECUTE FUNCTION forbid_posting_mutation();

CREATE TRIGGER trg_journals_immutable
    BEFORE UPDATE OR DELETE ON journals
    FOR EACH ROW EXECUTE FUNCTION forbid_posting_mutation();

-- Every journal must balance to zero, checked at commit time.
CREATE FUNCTION check_journal_balanced() RETURNS trigger AS $$
DECLARE
    total NUMERIC(19,4);
BEGIN
    SELECT COALESCE(SUM(amount), 0) INTO total FROM postings WHERE journal_id = NEW.journal_id;
    IF total <> 0 THEN
        RAISE EXCEPTION 'journal % does not balance (sum=%)', NEW.journal_id, total;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_postings_balanced
    AFTER INSERT ON postings
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_journal_balanced();

-- Opening balances that existed before the ledger become an auditable opening journal.
INSERT INTO journals (id, idempotency_key, journal_type, reference, description, currency, created_by)
SELECT gen_random_uuid(), 'opening-balance-' || id, 'OPENING_BALANCE', 'OPEN-' || id,
       'Migrated opening balance', currency, 'migration'
FROM accounts WHERE balance <> 0;

INSERT INTO postings (journal_id, account_id, system_account, amount, balance_after)
SELECT j.id, a.id, NULL, a.balance, a.balance
FROM accounts a JOIN journals j ON j.idempotency_key = 'opening-balance-' || a.id
UNION ALL
SELECT j.id, NULL, 'OPENING_EQUITY', -a.balance, NULL
FROM accounts a JOIN journals j ON j.idempotency_key = 'opening-balance-' || a.id;

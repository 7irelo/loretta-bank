package com.lorettabank.account.ledger;

/** Internal ledger accounts that balance customer postings. */
public enum SystemAccount {
    /** Physical or simulated cash entering and leaving the bank. */
    CASH_CLEARING,
    /** Balances that existed before the ledger was introduced. */
    OPENING_EQUITY
}

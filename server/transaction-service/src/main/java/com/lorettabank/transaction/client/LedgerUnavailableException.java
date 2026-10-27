package com.lorettabank.transaction.client;

/** The ledger's answer is unknown (timeout, 5xx, network); retry with the same key. */
public class LedgerUnavailableException extends RuntimeException {

    public LedgerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.shan.cyber.tech.financetracker.transaction.domain.model;

public enum TransactionType {

    INCOME,
    EXPENSE,
    TRANSFER_IN,
    TRANSFER_OUT;

    public boolean isDebit() {
        return this == EXPENSE || this == TRANSFER_OUT;
    }

    public boolean isCredit() {
        return this == INCOME || this == TRANSFER_IN;
    }

    public boolean isTransfer() {
        return this == TRANSFER_IN || this == TRANSFER_OUT;
    }
}

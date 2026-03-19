package com.shan.cyber.tech.financetracker.transaction.domain.model;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.AuditInfo;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;

import java.time.LocalDate;
import java.util.Objects;

public class Transaction {

    private TransactionId id;
    private final UserId userId;
    private final AccountId accountId;
    private CategoryId categoryId;
    private Money amount;
    private final TransactionType type;
    private LocalDate transactionDate;
    private String description;
    private String merchantName;
    private String referenceNumber;
    private final boolean isRecurring;
    private TransactionId transferPairId;
    private boolean isReconciled;
    private AuditInfo auditInfo;

    public Transaction(TransactionId id, UserId userId, AccountId accountId, CategoryId categoryId,
                       Money amount, TransactionType type, LocalDate transactionDate,
                       String description, String merchantName, String referenceNumber,
                       boolean isRecurring, TransactionId transferPairId, boolean isReconciled,
                       AuditInfo auditInfo) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.transactionDate = Objects.requireNonNull(transactionDate, "transactionDate must not be null");
        this.description = description;
        this.merchantName = merchantName;
        this.referenceNumber = referenceNumber;
        this.isRecurring = isRecurring;
        this.transferPairId = transferPairId;
        this.isReconciled = isReconciled;
        this.auditInfo = auditInfo;
    }

    public static Transaction create(UserId userId, AccountId accountId, CategoryId categoryId,
                                      Money amount, TransactionType type, LocalDate transactionDate,
                                      String description, String merchantName, String referenceNumber) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }
        return new Transaction(null, userId, accountId, categoryId, amount, type, transactionDate,
                description, merchantName, referenceNumber, false, null, false, null);
    }

    public Money updateDetails(CategoryId categoryId, Money newAmount, LocalDate transactionDate,
                               String description, String merchantName, String referenceNumber) {
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        Objects.requireNonNull(newAmount, "amount must not be null");
        Objects.requireNonNull(transactionDate, "transactionDate must not be null");
        if (!newAmount.isPositive()) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }
        Money oldAmount = this.amount;
        this.categoryId = categoryId;
        this.amount = newAmount;
        this.transactionDate = transactionDate;
        this.description = description;
        this.merchantName = merchantName;
        this.referenceNumber = referenceNumber;
        return oldAmount;
    }

    public void linkTransferPair(TransactionId pairId) {
        this.transferPairId = Objects.requireNonNull(pairId, "pairId must not be null");
    }

    public void unlinkTransferPair() {
        this.transferPairId = null;
    }

    public void reconcile(boolean reconciled) {
        this.isReconciled = reconciled;
    }

    public TransactionId getId() { return id; }
    public UserId getUserId() { return userId; }
    public AccountId getAccountId() { return accountId; }
    public CategoryId getCategoryId() { return categoryId; }
    public Money getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public String getDescription() { return description; }
    public String getMerchantName() { return merchantName; }
    public String getReferenceNumber() { return referenceNumber; }
    public boolean isRecurring() { return isRecurring; }
    public TransactionId getTransferPairId() { return transferPairId; }
    public boolean isReconciled() { return isReconciled; }
    public AuditInfo getAuditInfo() { return auditInfo; }

    public void setId(TransactionId id) { this.id = id; }
    public void setAuditInfo(AuditInfo auditInfo) { this.auditInfo = auditInfo; }
}

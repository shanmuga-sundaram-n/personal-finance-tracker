package com.shan.cyber.tech.financetracker.transaction.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.shared.domain.model.AuditInfo;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.Money;
import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.model.Transaction;
import com.shan.cyber.tech.financetracker.transaction.domain.model.TransactionType;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransactionView;
import org.springframework.stereotype.Component;

@Component
public class TransactionJpaMapper {

    public Transaction toDomain(TransactionJpaEntity entity, String currency) {
        AuditInfo auditInfo = new AuditInfo(
                entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getCreatedBy(), entity.getUpdatedBy());

        return new Transaction(
                new TransactionId(entity.getId()),
                new UserId(entity.getUserId()),
                new AccountId(entity.getAccountId()),
                new CategoryId(entity.getCategoryId()),
                Money.of(entity.getAmount(), currency),
                TransactionType.valueOf(entity.getTransactionType()),
                entity.getTransactionDate(),
                entity.getDescription(),
                entity.getMerchantName(),
                entity.getReferenceNumber(),
                entity.isRecurring(),
                entity.getTransferPairId() != null ? new TransactionId(entity.getTransferPairId()) : null,
                entity.isReconciled(),
                auditInfo);
    }

    public TransactionJpaEntity toJpaEntity(Transaction transaction) {
        TransactionJpaEntity entity = new TransactionJpaEntity();
        if (transaction.getId() != null) {
            entity.setId(transaction.getId().value());
        }
        entity.setUserId(transaction.getUserId().value());
        entity.setAccountId(transaction.getAccountId().value());
        entity.setCategoryId(transaction.getCategoryId().value());
        entity.setAmount(transaction.getAmount().amount());
        entity.setTransactionType(transaction.getType().name());
        entity.setTransactionDate(transaction.getTransactionDate());
        entity.setDescription(transaction.getDescription());
        entity.setMerchantName(transaction.getMerchantName());
        entity.setReferenceNumber(transaction.getReferenceNumber());
        entity.setRecurring(transaction.isRecurring());
        entity.setTransferPairId(transaction.getTransferPairId() != null ? transaction.getTransferPairId().value() : null);
        entity.setReconciled(transaction.isReconciled());
        return entity;
    }

    public TransactionView toView(TransactionViewProjection projection) {
        return new TransactionView(
                projection.getId(),
                projection.getAccountId(),
                projection.getAccountName(),
                projection.getCategoryId(),
                projection.getCategoryName(),
                projection.getAmount().toPlainString(),
                projection.getCurrency(),
                projection.getTransactionType(),
                projection.getTransactionDate(),
                projection.getDescription(),
                projection.getMerchantName(),
                projection.getReferenceNumber(),
                projection.getTransferPairId(),
                projection.getIsRecurring(),
                projection.getIsReconciled(),
                projection.getCreatedAt());
    }
}

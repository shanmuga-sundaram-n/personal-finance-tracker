package com.shan.cyber.tech.financetracker.account.adapter.outbound.crosscontext;

import com.shan.cyber.tech.financetracker.account.domain.port.outbound.AccountTransactionCountPort;
import com.shan.cyber.tech.financetracker.shared.domain.model.AccountId;
import com.shan.cyber.tech.financetracker.transaction.adapter.outbound.persistence.TransactionJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class TransactionCountAdapter implements AccountTransactionCountPort {

    private final TransactionJpaRepository transactionJpaRepository;

    public TransactionCountAdapter(TransactionJpaRepository transactionJpaRepository) {
        this.transactionJpaRepository = transactionJpaRepository;
    }

    @Override
    public boolean hasTransactions(AccountId accountId) {
        return transactionJpaRepository.existsByAccountId(accountId.value());
    }
}

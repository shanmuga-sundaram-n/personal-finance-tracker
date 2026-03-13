package com.shan.cyber.tech.financetracker.transaction.adapter.outbound.persistence;

import com.shan.cyber.tech.financetracker.account.adapter.outbound.persistence.AccountJpaEntity;
import com.shan.cyber.tech.financetracker.account.adapter.outbound.persistence.AccountJpaRepository;
import com.shan.cyber.tech.financetracker.shared.domain.model.CategoryId;
import com.shan.cyber.tech.financetracker.shared.domain.model.TransactionId;
import com.shan.cyber.tech.financetracker.shared.domain.model.UserId;
import com.shan.cyber.tech.financetracker.transaction.domain.model.Transaction;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransactionView;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionFilter;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPage;
import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.CategorySpending;
import com.shan.cyber.tech.financetracker.transaction.domain.port.outbound.TransactionPersistencePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class TransactionPersistenceAdapter implements TransactionPersistencePort {

    private final TransactionJpaRepository repository;
    private final AccountJpaRepository accountRepository;
    private final TransactionJpaMapper mapper;

    public TransactionPersistenceAdapter(TransactionJpaRepository repository,
                                          AccountJpaRepository accountRepository,
                                          TransactionJpaMapper mapper) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.mapper = mapper;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionJpaEntity entity = mapper.toJpaEntity(transaction);
        TransactionJpaEntity saved = repository.save(entity);
        String currency = fetchCurrency(saved.getAccountId());
        return mapper.toDomain(saved, currency);
    }

    @Override
    public List<Transaction> saveAll(List<Transaction> transactions) {
        List<TransactionJpaEntity> entities = transactions.stream()
                .map(mapper::toJpaEntity)
                .toList();
        List<TransactionJpaEntity> saved = repository.saveAll(entities);
        List<Transaction> result = new ArrayList<>();
        for (TransactionJpaEntity entity : saved) {
            String currency = fetchCurrency(entity.getAccountId());
            result.add(mapper.toDomain(entity, currency));
        }
        return result;
    }

    @Override
    public Optional<Transaction> findById(TransactionId id, UserId userId) {
        return repository.findByIdAndUserId(id.value(), userId.value())
                .map(entity -> {
                    String currency = fetchCurrency(entity.getAccountId());
                    return mapper.toDomain(entity, currency);
                });
    }

    @Override
    public Optional<TransactionView> findViewById(TransactionId id, UserId userId) {
        return repository.findViewByIdAndUserId(id.value(), userId.value())
                .map(mapper::toView);
    }

    @Override
    public TransactionPage findByFilter(TransactionFilter filter, int page, int size) {
        Page<TransactionViewProjection> jpaPage = repository.findByFilter(
                filter.userId().value(),
                filter.accountId() != null ? filter.accountId().value() : null,
                filter.categoryId() != null ? filter.categoryId().value() : null,
                filter.type() != null ? filter.type().name() : null,
                filter.fromDate(),
                filter.toDate(),
                PageRequest.of(page, size));

        List<TransactionView> views = jpaPage.getContent().stream()
                .map(mapper::toView)
                .toList();

        return new TransactionPage(views, jpaPage.getNumber(), jpaPage.getSize(),
                jpaPage.getTotalElements(), jpaPage.getTotalPages());
    }

    @Override
    public void delete(TransactionId id) {
        repository.deleteById(id.value());
    }

    @Override
    public BigDecimal sumExpenseAmount(UserId userId, CategoryId categoryId, LocalDate from, LocalDate to) {
        return repository.sumExpenseAmount(userId.value(), categoryId.value(), from, to);
    }

    @Override
    public BigDecimal sumByType(UserId userId, String transactionType, LocalDate from, LocalDate to) {
        return repository.sumByType(userId.value(), transactionType, from, to);
    }

    @Override
    public List<CategorySpending> sumByCategory(UserId userId, LocalDate from, LocalDate to, int limit) {
        List<CategorySpendingProjection> projections = repository.sumByCategory(userId.value(), from, to);
        return projections.stream()
                .limit(limit)
                .map(p -> new CategorySpending(p.getCategoryId(), p.getCategoryName(), p.getTotalAmount().toPlainString()))
                .toList();
    }

    @Override
    public TransactionPage findRecent(UserId userId, int limit) {
        Page<TransactionViewProjection> jpaPage = repository.findRecentByUserId(
                userId.value(), PageRequest.of(0, limit));
        List<TransactionView> views = jpaPage.getContent().stream()
                .map(mapper::toView)
                .toList();
        return new TransactionPage(views, 0, limit, jpaPage.getTotalElements(), jpaPage.getTotalPages());
    }

    private String fetchCurrency(Long accountId) {
        return accountRepository.findById(accountId)
                .map(AccountJpaEntity::getCurrency)
                .orElse("USD");
    }
}

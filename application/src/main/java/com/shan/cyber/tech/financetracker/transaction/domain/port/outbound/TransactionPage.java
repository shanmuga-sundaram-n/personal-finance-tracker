package com.shan.cyber.tech.financetracker.transaction.domain.port.outbound;

import com.shan.cyber.tech.financetracker.transaction.domain.port.inbound.TransactionView;

import java.util.List;

public record TransactionPage(
        List<TransactionView> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}

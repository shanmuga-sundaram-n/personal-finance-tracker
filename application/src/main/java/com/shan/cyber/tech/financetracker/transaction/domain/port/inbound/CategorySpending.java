package com.shan.cyber.tech.financetracker.transaction.domain.port.inbound;

public record CategorySpending(Long categoryId, String categoryName, String totalAmount) {
}

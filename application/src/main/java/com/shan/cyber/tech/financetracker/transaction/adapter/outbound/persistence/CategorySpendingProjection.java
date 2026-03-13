package com.shan.cyber.tech.financetracker.transaction.adapter.outbound.persistence;

import java.math.BigDecimal;

public interface CategorySpendingProjection {
    Long getCategoryId();
    String getCategoryName();
    BigDecimal getTotalAmount();
}

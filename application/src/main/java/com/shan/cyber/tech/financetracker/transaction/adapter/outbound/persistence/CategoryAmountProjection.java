package com.shan.cyber.tech.financetracker.transaction.adapter.outbound.persistence;

import java.math.BigDecimal;

public interface CategoryAmountProjection {
    Long getCategoryId();
    BigDecimal getTotalAmount();
}

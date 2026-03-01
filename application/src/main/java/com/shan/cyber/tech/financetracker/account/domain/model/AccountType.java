package com.shan.cyber.tech.financetracker.account.domain.model;

import java.util.Objects;

public final class AccountType {

    private final Short id;
    private final String code;
    private final String name;
    private final boolean allowsNegativeBalance;
    private final boolean isLiability;

    public AccountType(Short id, String code, String name, boolean allowsNegativeBalance, boolean isLiability) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.allowsNegativeBalance = allowsNegativeBalance;
        this.isLiability = isLiability;
    }

    public Short getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isAllowsNegativeBalance() { return allowsNegativeBalance; }
    public boolean isLiability() { return isLiability; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AccountType that)) return false;
        return code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}

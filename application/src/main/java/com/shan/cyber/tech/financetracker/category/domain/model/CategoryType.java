package com.shan.cyber.tech.financetracker.category.domain.model;

import java.util.Objects;

public final class CategoryType {

    private final Short id;
    private final String code;
    private final String name;

    public CategoryType(Short id, String code, String name) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public Short getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CategoryType that)) return false;
        return code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}

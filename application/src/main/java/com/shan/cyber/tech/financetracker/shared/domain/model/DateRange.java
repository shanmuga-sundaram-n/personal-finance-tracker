package com.shan.cyber.tech.financetracker.shared.domain.model;

import java.time.LocalDate;

public record DateRange(LocalDate startDate, LocalDate endDate) {

    public DateRange {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate));
    }

    public boolean overlaps(DateRange other) {
        return !this.startDate.isAfter(other.endDate == null ? LocalDate.MAX : other.endDate)
                && !(endDate != null && endDate.isBefore(other.startDate));
    }
}

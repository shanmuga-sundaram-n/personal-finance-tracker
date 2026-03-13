package com.shan.cyber.tech.financetracker.reporting.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.reporting.domain.port.inbound.MonthlyTrendItem;

import java.util.List;

public record MonthlyTrendResponseDto(List<MonthlyTrendItem> months) {
}

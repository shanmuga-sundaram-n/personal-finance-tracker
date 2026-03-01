package com.shan.cyber.tech.financetracker.shared.domain.exception;

public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String errorCode, String message) {
        super(errorCode, message);
    }
}

package com.shan.cyber.tech.financetracker.transaction.adapter.inbound.web;

import com.shan.cyber.tech.financetracker.transaction.domain.model.TransactionType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Web-layer mapper responsible for converting inbound string values to
 * {@link TransactionType} domain enum values. Keeps the controller free
 * of direct domain-model enum knowledge and centralises invalid-value
 * handling as a 400 Bad Request.
 */
@Component
public class TransactionRequestMapper {

    /**
     * Converts a transaction type string to {@link TransactionType}.
     *
     * @param type the raw string value from the request (e.g. "EXPENSE")
     * @return the matching {@link TransactionType} enum constant
     * @throws ResponseStatusException with HTTP 400 if the value is null, blank,
     *                                  or does not match any known enum constant
     */
    public TransactionType toTransactionType(String type) {
        if (type == null || type.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transaction type must not be blank");
        }
        try {
            return TransactionType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid transaction type: '" + type + "'. Accepted values: INCOME, EXPENSE, TRANSFER_IN, TRANSFER_OUT");
        }
    }

    /**
     * Converts a nullable transaction type string to {@link TransactionType},
     * returning {@code null} when the input is {@code null}.
     * Used for optional filter parameters.
     *
     * @param type the raw string value, or {@code null}
     * @return the matching {@link TransactionType}, or {@code null} if type is {@code null}
     * @throws ResponseStatusException with HTTP 400 if the value is non-null but invalid
     */
    public TransactionType toTransactionTypeOrNull(String type) {
        if (type == null) {
            return null;
        }
        return toTransactionType(type);
    }
}

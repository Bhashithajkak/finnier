package com.example.finnier.gateway;

import com.example.finnier.enums.PaymentStatus;

import java.math.BigDecimal;

public record PaymentGatewayResponse(
        boolean success,
        String gatewayReference,
        String transactionId,
        PaymentStatus status,
        BigDecimal amount,
        String failureReason,
        String maskedCardNumber,
        String cardBrand,
        String savedCardToken
) {
    public static PaymentGatewayResponse success(String gatewayReference, String transactionId, BigDecimal amount) {
        return new PaymentGatewayResponse(true, gatewayReference, transactionId, PaymentStatus.COMPLETED, amount, null, null, null, null);
    }

    public static PaymentGatewayResponse failed(String gatewayReference, String failureReason) {
        return new PaymentGatewayResponse(false, gatewayReference, null, PaymentStatus.FAILED, null, failureReason, null, null, null);
    }

    public static PaymentGatewayResponse pending(String gatewayReference, BigDecimal amount) {
        return new PaymentGatewayResponse(true, gatewayReference, null, PaymentStatus.PENDING, amount, null, null, null, null);
    }
}

package com.example.finnier.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentGatewayResponse charge(PaymentGatewayRequest request);

    PaymentGatewayResponse refund(String gatewayReference, BigDecimal amount, String reason);

    PaymentGatewayResponse saveCard(String customerEmail, String cardToken);

    PaymentGatewayResponse chargeWithSavedCard(String savedCardToken, BigDecimal amount, String orderReference, String idempotencyKey);

    boolean verifyWebhookSignature(String payload, String signatureHeader);

    String generateWebhookSignature(String payload);
}

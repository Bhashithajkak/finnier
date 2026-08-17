package com.example.finnier.gateway;

import com.example.finnier.enums.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Primary
public class MockPaymentGateway implements PaymentGateway {

    @Value("${payment.webhook.secret}")
    private String webhookSecret;

    // Simulated token vault for saved cards
    private final ConcurrentHashMap<String, SavedCardDetails> savedCardsVault = new ConcurrentHashMap<>();

    public record SavedCardDetails(String token, String maskedCard, String brand, String customerEmail) {}

    @Override
    public PaymentGatewayResponse charge(PaymentGatewayRequest request) {
        log.info("MockPaymentGateway: Processing charge for orderId={}, amount={}, email={}",
                request.orderId(), request.amount(), request.customerEmail());

        String gatewayRef = "MOCK_GW_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String cardToken = request.cardToken() != null ? request.cardToken() : "4242";

        // Simulate failure test cases
        if (cardToken.endsWith("0002") || cardToken.contains("insufficient_funds")) {
            log.warn("MockPaymentGateway: Charge failed due to insufficient funds for orderId={}", request.orderId());
            return PaymentGatewayResponse.failed(gatewayRef, "Card declined: Insufficient funds");
        } else if (cardToken.endsWith("0003") || cardToken.contains("expired_card")) {
            log.warn("MockPaymentGateway: Charge failed due to expired card for orderId={}", request.orderId());
            return PaymentGatewayResponse.failed(gatewayRef, "Card declined: Expired card");
        } else if (cardToken.endsWith("0004") || cardToken.contains("fraudulent")) {
            log.warn("MockPaymentGateway: Charge failed due to suspected fraud for orderId={}", request.orderId());
            return PaymentGatewayResponse.failed(gatewayRef, "Card declined: Suspected fraudulent transaction");
        }

        String transactionId = "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        log.info("MockPaymentGateway: Charge successful. gatewayRef={}, txnId={}", gatewayRef, transactionId);

        return new PaymentGatewayResponse(
                true,
                gatewayRef,
                transactionId,
                PaymentStatus.COMPLETED,
                request.amount(),
                null,
                "**** **** **** " + (cardToken.length() >= 4 ? cardToken.substring(cardToken.length() - 4) : "4242"),
                "VISA",
                null
        );
    }

    @Override
    public PaymentGatewayResponse refund(String gatewayReference, BigDecimal amount, String reason) {
        log.info("MockPaymentGateway: Processing refund for gatewayRef={}, amount={}, reason={}",
                gatewayReference, amount, reason);

        if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentGatewayResponse.failed(gatewayReference, "Refund amount must be greater than zero");
        }

        String refundTxnId = "REF_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return new PaymentGatewayResponse(
                true,
                gatewayReference,
                refundTxnId,
                PaymentStatus.REFUNDED,
                amount,
                null,
                null,
                null,
                null
        );
    }

    @Override
    public PaymentGatewayResponse saveCard(String customerEmail, String cardToken) {
        log.info("MockPaymentGateway: Tokenizing and saving card for customer={}", customerEmail);

        String token = "tok_saved_" + UUID.randomUUID().toString().replace("-", "");
        String last4 = (cardToken != null && cardToken.length() >= 4)
                ? cardToken.substring(cardToken.length() - 4)
                : "4242";
        String maskedCard = "**** **** **** " + last4;
        String brand = "VISA";

        savedCardsVault.put(token, new SavedCardDetails(token, maskedCard, brand, customerEmail));

        return new PaymentGatewayResponse(
                true,
                null,
                null,
                PaymentStatus.COMPLETED,
                null,
                null,
                maskedCard,
                brand,
                token
        );
    }

    @Override
    public PaymentGatewayResponse chargeWithSavedCard(String savedCardToken, BigDecimal amount, String orderReference, String idempotencyKey) {
        log.info("MockPaymentGateway: Recurring / Saved card charge for token={}, amount={}, orderRef={}",
                savedCardToken, amount, orderReference);

        SavedCardDetails cardDetails = savedCardsVault.get(savedCardToken);
        if (cardDetails == null && !savedCardToken.startsWith("tok_saved_")) {
            return PaymentGatewayResponse.failed(null, "Invalid or expired saved card token");
        }

        String gatewayRef = "MOCK_GW_RECURRING_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String transactionId = "TXN_REC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        return new PaymentGatewayResponse(
                true,
                gatewayRef,
                transactionId,
                PaymentStatus.COMPLETED,
                amount,
                null,
                cardDetails != null ? cardDetails.maskedCard() : "**** **** **** 4242",
                cardDetails != null ? cardDetails.brand() : "VISA",
                savedCardToken
        );
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        if (payload == null || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            String expectedSignature = generateWebhookSignature(payload);
            // Constant time comparison to protect against timing attacks
            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("MockPaymentGateway: Error verifying webhook signature", e);
            return false;
        }
    }

    @Override
    public String generateWebhookSignature(String payload) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            );
            hmac.init(secretKey);
            byte[] hmacBytes = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC-SHA256 signature", e);
        }
    }
}

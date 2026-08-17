package com.example.finnier.controller;

import com.example.finnier.dto.*;
import com.example.finnier.enums.PaymentStatus;
import com.example.finnier.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Management", description = "Endpoints for processing payments, webhooks, refunds and recurring cards")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    @Operation(summary = "Initiate a payment for an order", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentInitiateRequest request) {
        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm payment after gateway return/redirect", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PaymentResponse> confirmPayment(@Valid @RequestBody PaymentConfirmRequest request) {
        PaymentResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    @Operation(summary = "Gateway Webhook Callback endpoint (HMAC signature protected)")
    public ResponseEntity<PaymentResponse> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Signature", required = false) String altSignature
    ) {
        String effectiveSignature = signature != null ? signature : altSignature;
        PaymentResponse response = paymentService.handleWebhook(payload, effectiveSignature);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Refund a completed payment (Admin/Staff only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PaymentResponse> refundPayment(@Valid @RequestBody RefundRequest request) {
        PaymentResponse response = paymentService.refundPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save-card")
    @Operation(summary = "Tokenize and save card for future recurring charges", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<SavedCardResponse> saveCard(@Valid @RequestBody SaveCardRequest request) {
        SavedCardResponse response = paymentService.saveCard(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/charge-saved")
    @Operation(summary = "Pay for an order using a previously saved card token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PaymentResponse> chargeWithSavedCard(@Valid @RequestBody SavedCardChargeRequest request) {
        PaymentResponse response = paymentService.chargeWithSavedCard(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment details for an order", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mock-trigger-async-webhook")
    @Operation(summary = "Simulate async gateway webhook after a delay (Sandbox/Testing helper)")
    public ResponseEntity<String> triggerAsyncWebhookSimulation(
            @RequestParam String gatewayReference,
            @RequestParam(defaultValue = "COMPLETED") PaymentStatus status,
            @RequestParam(required = false) String failureReason,
            @RequestParam(defaultValue = "1") long delaySeconds
    ) {
        paymentService.triggerAsyncWebhookSimulation(gatewayReference, status, failureReason, delaySeconds);
        return ResponseEntity.ok("Async webhook simulation scheduled to execute in " + delaySeconds + " seconds.");
    }
}

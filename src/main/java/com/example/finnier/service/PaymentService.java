package com.example.finnier.service;

import com.example.finnier.dto.*;
import com.example.finnier.dto.event.OrderStatusUpdateEvent;
import com.example.finnier.dto.event.PaymentCompletedEvent;
import com.example.finnier.dto.event.PaymentFailedEvent;
import com.example.finnier.dto.event.PaymentRefundedEvent;
import com.example.finnier.entity.Order;
import com.example.finnier.entity.Payment;
import com.example.finnier.enums.PaymentMethod;
import com.example.finnier.enums.PaymentStatus;
import com.example.finnier.exception.*;
import com.example.finnier.gateway.PaymentGateway;
import com.example.finnier.gateway.PaymentGatewayRequest;
import com.example.finnier.gateway.PaymentGatewayResponse;
import com.example.finnier.messaging.OrderEventPublisher;
import com.example.finnier.messaging.PaymentEventPublisher;
import com.example.finnier.repository.OrderRepository;
import com.example.finnier.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentEventPublisher paymentEventPublisher;
    private final OrderEventPublisher orderEventPublisher;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService asyncExecutor = Executors.newSingleThreadScheduledExecutor();

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            PaymentGateway paymentGateway,
            PaymentEventPublisher paymentEventPublisher,
            OrderEventPublisher orderEventPublisher,
            ObjectMapper objectMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.paymentEventPublisher = paymentEventPublisher;
        this.orderEventPublisher = orderEventPublisher;
        this.objectMapper = objectMapper;
    }

//  Initiates a payment for an existing order with idempotency check and security validations.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse initiatePayment(PaymentInitiateRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + request.orderId()));

        validateOrderOwnershipOrAdmin(order);

        // Check if order is already paid
        if (order.getPaymentStatus() == PaymentStatus.COMPLETED || order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Order is already paid. Order ID: " + order.getOrderId());
        }

        // Idempotency check: if key is provided and payment already exists, return existing
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existingPayment.isPresent()) {
                log.info("Returning existing payment for idempotencyKey: {}", request.idempotencyKey());
                return toPaymentResponse(existingPayment.get());
            }
        }

        // Check if there is already an existing PENDING payment for this order
        Payment payment = paymentRepository.findByOrderOrderId(order.getOrderId())
                .orElse(Payment.builder()
                        .order(order)
                        .amount(order.getTotalAmount())
                        .paymentMethod(request.paymentMethod())
                        .paymentStatus(PaymentStatus.PENDING)
                        .idempotencyKey(request.idempotencyKey())
                        .build());

        payment.setPaymentMethod(request.paymentMethod());
        payment.setAmount(order.getTotalAmount());
        payment.setIdempotencyKey(request.idempotencyKey());

        PaymentGatewayRequest gatewayRequest = new PaymentGatewayRequest(
                order.getOrderId(),
                order.getTotalAmount(),
                "LKR",
                order.getCustomer().getUser().getEmail(),
                request.cardToken(),
                "Payment for Order #" + order.getOrderId(),
                request.idempotencyKey()
        );

        PaymentGatewayResponse gatewayResponse = paymentGateway.charge(gatewayRequest);

        payment.setGatewayReference(gatewayResponse.gatewayReference());
        payment.setTransactionId(gatewayResponse.transactionId());

        if (gatewayResponse.success() && gatewayResponse.status() == PaymentStatus.COMPLETED) {
            payment.setPaymentStatus(PaymentStatus.COMPLETED);
            payment.setFailureReason(null);
            Payment savedPayment = paymentRepository.save(payment);

            applySuccessfulPayment(order, savedPayment);
            return toPaymentResponse(savedPayment);
        } else if (gatewayResponse.status() == PaymentStatus.PENDING) {
            payment.setPaymentStatus(PaymentStatus.PENDING);
            Payment savedPayment = paymentRepository.save(payment);
            return toPaymentResponse(savedPayment);
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason(gatewayResponse.failureReason());
            Payment savedPayment = paymentRepository.save(payment);

            publishPaymentFailure(order, savedPayment);
            return toPaymentResponse(savedPayment);
        }
    }


//      Confirms a payment synchronously using gateway reference. Idempotent.

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse confirmPayment(PaymentConfirmRequest request) {
        Payment payment = paymentRepository.findByGatewayReference(request.gatewayReference())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with gateway reference: " + request.gatewayReference()));

        validateOrderOwnershipOrAdmin(payment.getOrder());

        // Idempotency: if already completed, do not re-apply
        if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            log.info("Payment already completed for gatewayReference: {}", request.gatewayReference());
            return toPaymentResponse(payment);
        }

        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        if (payment.getTransactionId() == null) {
            payment.setTransactionId("TXN_CONFIRMED_" + System.currentTimeMillis());
        }
        Payment savedPayment = paymentRepository.save(payment);

        applySuccessfulPayment(payment.getOrder(), savedPayment);

        return toPaymentResponse(savedPayment);
    }


//  Handles webhook callbacks sent asynchronously by the payment gateway with HMAC verification.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse handleWebhook(String payload, String signature) {
        log.info("Received payment webhook callback");

        if (!paymentGateway.verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid webhook signature received");
            throw new InvalidWebhookSignatureException("Invalid webhook signature");
        }

        try {
            WebhookEventPayload event = objectMapper.readValue(payload, WebhookEventPayload.class);
            Payment payment = paymentRepository.findByGatewayReference(event.gatewayReference())
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found for webhook gateway ref: " + event.gatewayReference()));

            Order order = payment.getOrder();

            if (event.status() == PaymentStatus.COMPLETED) {
                // Idempotency check
                if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
                    log.info("Webhook duplicate ignored: Payment already completed for ref: {}", event.gatewayReference());
                    return toPaymentResponse(payment);
                }

                payment.setPaymentStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(event.transactionId());
                payment.setFailureReason(null);
                Payment savedPayment = paymentRepository.save(payment);

                applySuccessfulPayment(order, savedPayment);
                return toPaymentResponse(savedPayment);
            } else if (event.status() == PaymentStatus.FAILED) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setFailureReason(event.failureReason());
                Payment savedPayment = paymentRepository.save(payment);

                publishPaymentFailure(order, savedPayment);
                return toPaymentResponse(savedPayment);
            } else if (event.status() == PaymentStatus.REFUNDED) {
                return refundPayment(new RefundRequest(payment.getPaymentId(), event.amount(), "Webhook Refund Notification"));
            }

            return toPaymentResponse(payment);
        } catch (InvalidWebhookSignatureException | PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse and process webhook payload", e);
            throw new PaymentProcessingException("Error processing webhook payload", e);
        }
    }

//  Refunds an existing completed payment. Restricts to Admin or Staff.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse refundPayment(RefundRequest request) {
        validateAdminOrStaff();

        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + request.paymentId()));

        if (payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot refund a payment with status: " + payment.getPaymentStatus());
        }

        BigDecimal refundAmount = request.amount() != null ? request.amount() : payment.getAmount();
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0 || refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Invalid refund amount. Must be between 0.01 and " + payment.getAmount());
        }

        PaymentGatewayResponse gatewayResponse = paymentGateway.refund(
                payment.getGatewayReference(),
                refundAmount,
                request.reason()
        );

        if (!gatewayResponse.success()) {
            throw new PaymentProcessingException("Refund failed by gateway: " + gatewayResponse.failureReason());
        }

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        Payment savedPayment = paymentRepository.save(payment);

        Order order = payment.getOrder();
        Order.OrderStatus previousOrderStatus = order.getOrderStatus();
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Publish Order Status update
        OrderStatusUpdateEvent statusUpdateEvent = new OrderStatusUpdateEvent(
                order.getOrderId(),
                order.getCustomer().getCustomerId(),
                order.getCustomer().getUser().getEmail(),
                previousOrderStatus,
                Order.OrderStatus.CANCELLED,
                PaymentStatus.REFUNDED,
                LocalDateTime.now()
        );
        orderEventPublisher.publishOrderStatusUpdated(statusUpdateEvent);

        // Publish Payment Refunded event
        PaymentRefundedEvent refundEvent = new PaymentRefundedEvent(
                savedPayment.getPaymentId(),
                order.getOrderId(),
                order.getCustomer().getCustomerId(),
                order.getCustomer().getUser().getEmail(),
                refundAmount,
                request.reason(),
                LocalDateTime.now()
        );
        paymentEventPublisher.publishPaymentRefunded(refundEvent);

        return toPaymentResponse(savedPayment);
    }

//  Saves a card token in the customer vault for future recurring payments.
    public SavedCardResponse saveCard(SaveCardRequest request) {
        String email = getAuthenticatedUserEmail();
        PaymentGatewayResponse response = paymentGateway.saveCard(email, request.cardToken());

        return new SavedCardResponse(
                response.savedCardToken(),
                response.maskedCardNumber(),
                response.cardBrand()
        );
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse chargeWithSavedCard(SavedCardChargeRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + request.orderId()));

        validateOrderOwnershipOrAdmin(order);

        if (order.getPaymentStatus() == PaymentStatus.COMPLETED || order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Order is already paid. Order ID: " + order.getOrderId());
        }

        Payment payment = paymentRepository.findByOrderOrderId(order.getOrderId())
                .orElse(Payment.builder()
                        .order(order)
                        .amount(order.getTotalAmount())
                        .paymentMethod(PaymentMethod.SAVED_CARD)
                        .paymentStatus(PaymentStatus.PENDING)
                        .savedCardToken(request.savedCardToken())
                        .idempotencyKey(request.idempotencyKey())
                        .build());

        payment.setPaymentMethod(PaymentMethod.SAVED_CARD);
        payment.setSavedCardToken(request.savedCardToken());

        PaymentGatewayResponse gatewayResponse = paymentGateway.chargeWithSavedCard(
                request.savedCardToken(),
                order.getTotalAmount(),
                "ORDER_" + order.getOrderId(),
                request.idempotencyKey()
        );

        payment.setGatewayReference(gatewayResponse.gatewayReference());
        payment.setTransactionId(gatewayResponse.transactionId());

        if (gatewayResponse.success()) {
            payment.setPaymentStatus(PaymentStatus.COMPLETED);
            Payment savedPayment = paymentRepository.save(payment);
            applySuccessfulPayment(order, savedPayment);
            return toPaymentResponse(savedPayment);
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason(gatewayResponse.failureReason());
            Payment savedPayment = paymentRepository.save(payment);
            publishPaymentFailure(order, savedPayment);
            return toPaymentResponse(savedPayment);
        }
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order id: " + orderId));

        validateOrderOwnershipOrAdmin(payment.getOrder());
        return toPaymentResponse(payment);
    }

    public void triggerAsyncWebhookSimulation(String gatewayReference, PaymentStatus status, String failureReason, long delaySeconds) {
        asyncExecutor.schedule(() -> {
            try {
                Payment payment = paymentRepository.findByGatewayReference(gatewayReference).orElse(null);
                if (payment == null) {
                    log.warn("Async webhook simulation skipped: payment not found for gatewayRef: {}", gatewayReference);
                    return;
                }

                String transactionId = "TXN_ASYNC_" + System.currentTimeMillis();
                WebhookEventPayload payloadObj = new WebhookEventPayload(
                        "payment." + status.name().toLowerCase(),
                        gatewayReference,
                        transactionId,
                        status,
                        payment.getAmount(),
                        failureReason,
                        System.currentTimeMillis()
                );

                String payloadJson = objectMapper.writeValueAsString(payloadObj);
                String signature = paymentGateway.generateWebhookSignature(payloadJson);

                log.info("Simulating async gateway webhook callback for gatewayRef: {}", gatewayReference);
                handleWebhook(payloadJson, signature);
            } catch (Exception e) {
                log.error("Async webhook simulation failed", e);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }


    private void applySuccessfulPayment(Order order, Payment payment) {
        Order.OrderStatus previousOrderStatus = order.getOrderStatus();
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setOrderStatus(Order.OrderStatus.PAID);
        orderRepository.save(order);

        OrderStatusUpdateEvent statusEvent = new OrderStatusUpdateEvent(
                order.getOrderId(),
                order.getCustomer().getCustomerId(),
                order.getCustomer().getUser().getEmail(),
                previousOrderStatus,
                Order.OrderStatus.PAID,
                PaymentStatus.COMPLETED,
                LocalDateTime.now()
        );
        orderEventPublisher.publishOrderStatusUpdated(statusEvent);

        PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                payment.getPaymentId(),
                order.getOrderId(),
                order.getCustomer().getCustomerId(),
                order.getCustomer().getUser().getEmail(),
                payment.getAmount(),
                payment.getTransactionId(),
                payment.getGatewayReference(),
                LocalDateTime.now()
        );
        paymentEventPublisher.publishPaymentCompleted(completedEvent);
    }

    private void publishPaymentFailure(Order order, Payment payment) {
        PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                payment.getPaymentId(),
                order.getOrderId(),
                order.getCustomer().getCustomerId(),
                order.getCustomer().getUser().getEmail(),
                payment.getAmount(),
                payment.getFailureReason(),
                LocalDateTime.now()
        );
        paymentEventPublisher.publishPaymentFailed(failedEvent);
    }

    private void validateOrderOwnershipOrAdmin(Order order) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedPaymentAccessException("User is not authenticated");
        }

        boolean isAdminOrStaff = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));

        if (!isAdminOrStaff) {
            String currentUserEmail = auth.getName();
            if (order.getCustomer() == null || order.getCustomer().getUser() == null ||
                    !order.getCustomer().getUser().getEmail().equalsIgnoreCase(currentUserEmail)) {
                throw new UnauthorizedPaymentAccessException("Access denied: You do not own this order.");
            }
        }
    }

    private void validateAdminOrStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedPaymentAccessException("User is not authenticated");
        }

        boolean isAdminOrStaff = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));

        if (!isAdminOrStaff) {
            throw new UnauthorizedPaymentAccessException("Access denied: Admin or Staff role required.");
        }
    }

    private String getAuthenticatedUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedPaymentAccessException("User is not authenticated");
        }
        return auth.getName();
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrder() != null ? payment.getOrder().getOrderId() : null,
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getPaymentStatus(),
                payment.getTransactionId(),
                payment.getGatewayReference(),
                payment.getFailureReason(),
                payment.getPaymentDate(),
                payment.getUpdatedAt()
        );
    }
}

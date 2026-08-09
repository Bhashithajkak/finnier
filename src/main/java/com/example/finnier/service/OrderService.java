package com.example.finnier.service;

import com.example.finnier.dto.*;
import com.example.finnier.dto.event.OrderCreatedEvent;
import com.example.finnier.dto.event.OrderStatusUpdateEvent;
import com.example.finnier.entity.*;
import com.example.finnier.enums.PaymentStatus;
import com.example.finnier.exception.InsufficientStockException;
import com.example.finnier.exception.OrderNotFoundException;
import com.example.finnier.exception.UnauthorizedOrderAccessException;
import com.example.finnier.messaging.OrderEventPublisher;
import com.example.finnier.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final CustomerRepository customerRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ShippingAddressRepository shippingAddressRepository,
            CustomerRepository customerRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            OrderEventPublisher orderEventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shippingAddressRepository = shippingAddressRepository;
        this.customerRepository = customerRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest orderRequest) {
        Customer customer = getCurrentCustomer();

        if (!orderRequest.hasValidShippingAddress()) {
            throw new IllegalArgumentException("A valid shipping address must be provided");
        }

        ShippingAddress shippingAddress = resolveShippingAddress(customer, orderRequest);

        Cart cart = cartRepository.findByCustomerCustomerId(customer.getCustomerId())
                .orElseThrow(() -> new IllegalStateException("Cart not found for customer"));

        List<CartItem> allCartItems = cartItemRepository.findAllByCartCartId(cart.getCartId());
        if (allCartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty. Cannot place an order.");
        }

        List<CartItem> itemsToOrder;
        if (orderRequest.cartItemIds() != null && !orderRequest.cartItemIds().isEmpty()) {
            itemsToOrder = allCartItems.stream()
                    .filter(item -> orderRequest.cartItemIds().contains(item.getCartItemId()))
                    .toList();
        } else {
            itemsToOrder = allCartItems;
        }

        if (itemsToOrder.isEmpty()) {
            throw new IllegalArgumentException("No valid cart items selected for order");
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setShippingAddress(shippingAddress);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);

        Order savedOrder = orderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderCreatedEvent.OrderItemEventInfo> eventItems = new ArrayList<>();

        for (CartItem cartItem : itemsToOrder) {
            Product product = cartItem.getProduct();
            int requestedQty = cartItem.getQuantity();

            if (product.getStatus() == Product.Status.OUT_OF_STOCK || product.getQuantity() < requestedQty) {
                throw new InsufficientStockException(
                        "Insufficient stock for product '" + product.getName() +
                                "'. Available: " + product.getQuantity() + ", Requested: " + requestedQty
                );
            }

            product.setQuantity(product.getQuantity() - requestedQty);
            if (product.getQuantity() == 0) {
                product.setStatus(Product.Status.OUT_OF_STOCK);
            }
            productRepository.save(product);

            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(requestedQty));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(requestedQty);
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(subtotal);

            OrderItem savedItem = orderItemRepository.save(orderItem);
            orderItems.add(savedItem);

            eventItems.add(new OrderCreatedEvent.OrderItemEventInfo(
                    product.getProductId(),
                    product.getName(),
                    requestedQty,
                    unitPrice,
                    subtotal
            ));

            cartItemRepository.delete(cartItem);
        }

        savedOrder.setTotalAmount(totalAmount);
        Order finalOrder = orderRepository.save(savedOrder);

        OrderCreatedEvent event = new OrderCreatedEvent(
                finalOrder.getOrderId(),
                customer.getCustomerId(),
                customer.getUser().getEmail(),
                totalAmount,
                finalOrder.getOrderStatus(),
                finalOrder.getPaymentStatus(),
                finalOrder.getOrderDate(),
                eventItems
        );
        orderEventPublisher.publishOrderCreated(event);

        return toOrderResponse(finalOrder, orderItems);
    }

    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        validateOrderOwnershipOrAdmin(order);

        List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);
        return toOrderResponse(order, items);
    }

    public List<OrderResponse> getMyOrders() {
        String email = getAuthenticatedUserEmail();
        List<Order> orders = orderRepository.findByCustomerUserEmail(email);
        return orders.stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
                    return toOrderResponse(order, items);
                })
                .toList();
    }

    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
                    return toOrderResponse(order, items);
                })
                .toList();
    }

    public Page<OrderResponse> getAllOrdersPaginated(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
            return toOrderResponse(order, items);
        });
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        Order.OrderStatus previousStatus = order.getOrderStatus();
        order.setOrderStatus(request.orderStatus());

        if (request.paymentStatus() != null) {
            order.setPaymentStatus(request.paymentStatus());
        }

        Order updatedOrder = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);

        OrderStatusUpdateEvent event = new OrderStatusUpdateEvent(
                updatedOrder.getOrderId(),
                updatedOrder.getCustomer().getCustomerId(),
                updatedOrder.getCustomer().getUser().getEmail(),
                previousStatus,
                updatedOrder.getOrderStatus(),
                updatedOrder.getPaymentStatus(),
                LocalDateTime.now()
        );
        orderEventPublisher.publishOrderStatusUpdated(event);

        return toOrderResponse(updatedOrder, items);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        validateOrderOwnershipOrAdmin(order);

        if (order.getOrderStatus() == Order.OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }

        if (order.getOrderStatus() == Order.OrderStatus.SHIPPED || order.getOrderStatus() == Order.OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel an order that has already been shipped or delivered");
        }

        Order.OrderStatus previousStatus = order.getOrderStatus();
        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);

        List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);
        for (OrderItem item : items) {
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() + item.getQuantity());
            if (product.getQuantity() > 0 && product.getStatus() == Product.Status.OUT_OF_STOCK) {
                product.setStatus(Product.Status.AVAILABLE);
            }
            productRepository.save(product);
        }

        Order cancelledOrder = orderRepository.save(order);

        OrderStatusUpdateEvent event = new OrderStatusUpdateEvent(
                cancelledOrder.getOrderId(),
                cancelledOrder.getCustomer().getCustomerId(),
                cancelledOrder.getCustomer().getUser().getEmail(),
                previousStatus,
                Order.OrderStatus.CANCELLED,
                cancelledOrder.getPaymentStatus(),
                LocalDateTime.now()
        );
        orderEventPublisher.publishOrderStatusUpdated(event);

        return toOrderResponse(cancelledOrder, items);
    }

    // Helper Methods

    private Customer getCurrentCustomer() {
        String email = getAuthenticatedUserEmail();
        return customerRepository.findByUserEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Customer profile not found for authenticated user: " + email));
    }

    private String getAuthenticatedUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedOrderAccessException("User is not authenticated");
        }
        return auth.getName();
    }

    private void validateOrderOwnershipOrAdmin(Order order) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedOrderAccessException("Unauthenticated request");
        }

        boolean isAdminOrStaff = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));

        if (!isAdminOrStaff) {
            String currentUserEmail = auth.getName();
            if (!order.getCustomer().getUser().getEmail().equalsIgnoreCase(currentUserEmail)) {
                throw new UnauthorizedOrderAccessException("Access denied: You do not own this order.");
            }
        }
    }

    private ShippingAddress resolveShippingAddress(Customer customer, OrderRequest orderRequest) {
        if (orderRequest.shippingAddressId() != null) {
            ShippingAddress address = shippingAddressRepository.findById(orderRequest.shippingAddressId())
                    .orElseThrow(() -> new EntityNotFoundException("Shipping address not found with id: " + orderRequest.shippingAddressId()));
            if (!address.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                throw new UnauthorizedOrderAccessException("Selected shipping address does not belong to current customer");
            }
            return address;
        }

        ShippingAddressDto dto = orderRequest.newShippingAddress();
        ShippingAddress newAddress = new ShippingAddress();
        newAddress.setCustomer(customer);
        newAddress.setRecipientName(dto.recipientName());
        newAddress.setPhoneNumber(dto.phoneNumber());
        newAddress.setAddressLine1(dto.addressLine1());
        newAddress.setAddressLine2(dto.addressLine2());
        newAddress.setCity(dto.city());
        newAddress.setDistrict(dto.district());
        newAddress.setPostalCode(dto.postalCode());
        newAddress.setCountry(dto.country());

        return shippingAddressRepository.save(newAddress);
    }

    private OrderResponse toOrderResponse(Order order, List<OrderItem> items) {
        ShippingAddress addr = order.getShippingAddress();
        ShippingAddressDto addressDto = addr != null ? new ShippingAddressDto(
                addr.getRecipientName(),
                addr.getPhoneNumber(),
                addr.getAddressLine1(),
                addr.getAddressLine2(),
                addr.getCity(),
                addr.getDistrict(),
                addr.getPostalCode(),
                addr.getCountry()
        ) : null;

        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getOrderItemId(),
                        item.getProduct().getProductId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getOrderId(),
                order.getCustomer().getCustomerId(),
                order.getCustomer().getUser().getEmail(),
                addressDto,
                order.getOrderDate(),
                order.getTotalAmount(),
                order.getOrderStatus(),
                order.getPaymentStatus(),
                itemResponses
        );
    }
}

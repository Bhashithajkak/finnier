package com.example.finnier.service;

import com.example.finnier.dto.OrderRequest;
import com.example.finnier.dto.OrderResponse;
import com.example.finnier.dto.OrderStatusUpdateRequest;
import com.example.finnier.dto.ShippingAddressDto;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ShippingAddressRepository shippingAddressRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Customer customer;
    private Product product;
    private Cart cart;
    private CartItem cartItem;
    private ShippingAddress shippingAddress;
    private Order order;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1L)
                .email("customer@example.com")
                .role(User.RoleType.CUSTOMER)
                .status(User.UserStatus.ACTIVE)
                .build();

        customer = Customer.builder().customerId(10L).user(user).build();

        product = Product.builder()
                .productId(100L)
                .name("Handmade Vase")
                .price(new BigDecimal("250.00"))
                .quantity(5)
                .status(Product.Status.AVAILABLE)
                .build();

        cart = new Cart();
        cart.setCartId(1L);
        cart.setCustomer(customer);

        cartItem = CartItem.builder()
                .cartItemId(200L)
                .cart(cart)
                .product(product)
                .quantity(2)
                .subtotal(new BigDecimal("500.00"))
                .build();

        shippingAddress = new ShippingAddress();
        shippingAddress.setAddressId(50L);
        shippingAddress.setCustomer(customer);
        shippingAddress.setRecipientName("John Doe");
        shippingAddress.setPhoneNumber("0771234567");
        shippingAddress.setAddressLine1("123 Main St");
        shippingAddress.setCity("Colombo");
        shippingAddress.setDistrict("Western");
        shippingAddress.setPostalCode("00100");
        shippingAddress.setCountry("Sri Lanka");

        order = new Order();
        order.setOrderId(1000L);
        order.setCustomer(customer);
        order.setShippingAddress(shippingAddress);
        order.setOrderDate(LocalDateTime.now());
        order.setOrderStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalAmount(new BigDecimal("500.00"));

        setAuthenticatedUser("customer@example.com", "ROLE_CUSTOMER");
    }

    private void setAuthenticatedUser(String email, String... roles) {
        var authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, authorities));
    }

    private OrderItem buildSavedOrderItem() {
        OrderItem oi = new OrderItem();
        oi.setOrderItemId(300L);
        oi.setOrder(order);
        oi.setProduct(product);
        oi.setQuantity(2);
        oi.setUnitPrice(new BigDecimal("250.00"));
        oi.setSubtotal(new BigDecimal("500.00"));
        return oi;
    }

    @Nested
    class PlaceOrder {

        private OrderRequest requestWithExistingAddress() {
            return new OrderRequest(null, 50L, null);
        }

        private OrderRequest requestWithNewAddress() {
            ShippingAddressDto dto = new ShippingAddressDto(
                    "Jane Doe", "0779876543", "456 Side St", null,
                    "Kandy", "Central", "20000", "Sri Lanka");
            return new OrderRequest(null, null, dto);
        }

        private void stubHappyPath() {
            when(customerRepository.findByUserEmail("customer@example.com")).thenReturn(Optional.of(customer));
            when(shippingAddressRepository.findById(50L)).thenReturn(Optional.of(shippingAddress));
            when(cartRepository.findByCustomerCustomerId(10L)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findAllByCartCartId(1L)).thenReturn(List.of(cartItem));
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(orderItemRepository.save(any(OrderItem.class))).thenReturn(buildSavedOrderItem());
            when(productRepository.save(any(Product.class))).thenReturn(product);
        }

        @Test
        void placeOrder_success_withExistingAddress() {
            stubHappyPath();

            OrderResponse response = orderService.placeOrder(requestWithExistingAddress());

            assertThat(response.orderId()).isEqualTo(1000L);
            assertThat(response.customerId()).isEqualTo(10L);
            assertThat(response.customerEmail()).isEqualTo("customer@example.com");
            assertThat(response.orderStatus()).isEqualTo(Order.OrderStatus.PENDING);
            assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
            verify(orderRepository, times(2)).save(any(Order.class));
            verify(cartItemRepository).delete(cartItem);
            verify(orderEventPublisher).publishOrderCreated(any(OrderCreatedEvent.class));
        }

        @Test
        void placeOrder_success_withNewAddress() {
            when(customerRepository.findByUserEmail("customer@example.com")).thenReturn(Optional.of(customer));
            when(cartRepository.findByCustomerCustomerId(10L)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findAllByCartCartId(1L)).thenReturn(List.of(cartItem));
            when(shippingAddressRepository.save(any(ShippingAddress.class))).thenReturn(shippingAddress);
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(orderItemRepository.save(any(OrderItem.class))).thenReturn(buildSavedOrderItem());
            when(productRepository.save(any(Product.class))).thenReturn(product);

            OrderResponse response = orderService.placeOrder(requestWithNewAddress());

            assertThat(response).isNotNull();
            verify(shippingAddressRepository).save(any(ShippingAddress.class));
        }

        @Test
        void placeOrder_success_withSelectedCartItemIds() {
            stubHappyPath();
            OrderResponse response = orderService.placeOrder(new OrderRequest(List.of(200L), 50L, null));
            assertThat(response).isNotNull();
            verify(orderItemRepository).save(any(OrderItem.class));
        }

        @Test
        void placeOrder_setsProductOutOfStock_whenStockExhausted() {
            product.setQuantity(2);
            when(customerRepository.findByUserEmail("customer@example.com")).thenReturn(Optional.of(customer));
            when(shippingAddressRepository.findById(50L)).thenReturn(Optional.of(shippingAddress));
            when(cartRepository.findByCustomerCustomerId(10L)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findAllByCartCartId(1L)).thenReturn(List.of(cartItem));
            when(orderRepository.save(any(Order.class))).thenReturn(order);
            when(orderItemRepository.save(any(OrderItem.class))).thenReturn(buildSavedOrderItem());

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            when(productRepository.save(captor.capture())).thenReturn(product);

            orderService.placeOrder(requestWithExistingAddress());

            assertThat(captor.getValue().getStatus()).isEqualTo(Product.Status.OUT_OF_STOCK);
            assertThat(captor.getValue().getQuantity()).isZero();
        }

        @Test
        void placeOrder_throwsException_whenNoShippingAddress() {
            when(customerRepository.findByUserEmail("customer@example.com")).thenReturn(Optional.of(customer));
            assertThatThrownBy(() -> orderService.placeOrder(new OrderRequest(null, null, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("valid shipping address");
        }

        @Test
        void placeOrder_throwsException_whenCartNotFound() {
            when(customerRepository.findByUserEmail("customer@example.com")).thenReturn(Optional.of(customer));
            when(shippingAddressRepository.findById(50L)).thenReturn(Optional.of(shippingAddress));
            when(cartRepository.findByCustomerCustomerId(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.placeOrder(requestWithExistingAddress()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cart not found");
        }

        @Test
        void placeOrder_throwsException_whenCartIsEmpty() {
            when(customerRepository.findByUserEmail("customer@example.com")).thenReturn(Optional.of(customer));
            when(shippingAddressRepository.findById(50L)).thenReturn(Optional.of(shippingAddress));
            when(cartRepository.findByCustomerCustomerId(10L)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findAllByCartCartId(1L)).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> orderService.placeOrder(requestWithExistingAddress()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cart is empty");
        }

        @Test
        void placeOrder_throwsException_whenSelectedItemsNotInCart() {
            when(customerRepository.findByUserEmail("customer@example.com")).thenReturn(Optional.of(customer));
            when(shippingAddressRepository.findById(50L)).thenReturn(Optional.of(shippingAddress));
            when(cartRepository.findByCustomerCustomerId(10L)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findAllByCartCartId(1L)).thenReturn(List.of(cartItem));

            assertThatThrownBy(() -> orderService.placeOrder(new OrderRequest(List.of(999L), 50L, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No valid cart items");
        }

        @Test
        void placeOrder_throwsException_whenProductOutOfStock() {
            product.setStatus(Product.Status.OUT_OF_STOCK);
            when(customerRepository.findByUserEmail("customer@example.com")).thenReturn(Optional.of(customer));
            when(shippingAddressRepository.findById(50L)).thenReturn(Optional.of(shippingAddress));
            when(cartRepository.findByCustomerCustomerId(10L)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findAllByCartCartId(1L)).thenReturn(List.of(cartItem));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            assertThatThrownBy(() -> orderService.placeOrder(requestWithExistingAddress()))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient stock");
        }

        @Test
        void placeOrder_throwsException_whenRequestedQtyExceedsStock() {
            product.setQuantity(1);
            when(customerRepository.findByUserEmail("customer@example.com")).thenReturn(Optional.of(customer));
            when(shippingAddressRepository.findById(50L)).thenReturn(Optional.of(shippingAddress));
            when(cartRepository.findByCustomerCustomerId(10L)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findAllByCartCartId(1L)).thenReturn(List.of(cartItem));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            assertThatThrownBy(() -> orderService.placeOrder(requestWithExistingAddress()))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Available: 1")
                    .hasMessageContaining("Requested: 2");
        }

        @Test
        void placeOrder_throwsException_whenShippingAddressOwnerMismatch() {
            shippingAddress.setCustomer(Customer.builder().customerId(99L).build());
            when(customerRepository.findByUserEmail("customer@example.com")).thenReturn(Optional.of(customer));
            when(shippingAddressRepository.findById(50L)).thenReturn(Optional.of(shippingAddress));

            assertThatThrownBy(() -> orderService.placeOrder(requestWithExistingAddress()))
                    .isInstanceOf(UnauthorizedOrderAccessException.class);
        }

        @Test
        void placeOrder_throwsException_whenCustomerNotFound() {
            when(customerRepository.findByUserEmail("customer@example.com")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> orderService.placeOrder(requestWithExistingAddress()))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        void placeOrder_publishesOrderCreatedEvent_withCorrectDetails() {
            stubHappyPath();
            orderService.placeOrder(requestWithExistingAddress());

            ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(orderEventPublisher).publishOrderCreated(captor.capture());

            OrderCreatedEvent event = captor.getValue();
            assertThat(event.orderId()).isEqualTo(1000L);
            assertThat(event.customerId()).isEqualTo(10L);
            assertThat(event.customerEmail()).isEqualTo("customer@example.com");
            assertThat(event.orderStatus()).isEqualTo(Order.OrderStatus.PENDING);
            assertThat(event.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(event.items()).hasSize(1);

            OrderCreatedEvent.OrderItemEventInfo item = event.items().get(0);
            assertThat(item.productId()).isEqualTo(100L);
            assertThat(item.productName()).isEqualTo("Handmade Vase");
            assertThat(item.quantity()).isEqualTo(2);
            assertThat(item.unitPrice()).isEqualByComparingTo("250.00");
            assertThat(item.subtotal()).isEqualByComparingTo("500.00");
        }
    }

    @Nested
    class GetOrderById {

        @Test
        void getOrderById_returnsOrder_forOwner() {
            when(orderRepository.findByIdWithDetails(1000L)).thenReturn(Optional.of(order));
            when(orderItemRepository.findByOrderOrderId(1000L)).thenReturn(List.of(buildSavedOrderItem()));
            assertThat(orderService.getOrderById(1000L).orderId()).isEqualTo(1000L);
        }

        @Test
        void getOrderById_returnsOrder_forAdmin() {
            setAuthenticatedUser("admin@example.com", "ROLE_ADMIN");
            when(orderRepository.findByIdWithDetails(1000L)).thenReturn(Optional.of(order));
            when(orderItemRepository.findByOrderOrderId(1000L)).thenReturn(List.of(buildSavedOrderItem()));
            assertThat(orderService.getOrderById(1000L).orderId()).isEqualTo(1000L);
        }

        @Test
        void getOrderById_returnsOrder_forStaff() {
            setAuthenticatedUser("staff@example.com", "ROLE_STAFF");
            when(orderRepository.findByIdWithDetails(1000L)).thenReturn(Optional.of(order));
            when(orderItemRepository.findByOrderOrderId(1000L)).thenReturn(List.of(buildSavedOrderItem()));
            assertThat(orderService.getOrderById(1000L).orderId()).isEqualTo(1000L);
        }

        @Test
        void getOrderById_throwsException_forNonOwner() {
            setAuthenticatedUser("another@example.com", "ROLE_CUSTOMER");
            when(orderRepository.findByIdWithDetails(1000L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.getOrderById(1000L))
                    .isInstanceOf(UnauthorizedOrderAccessException.class)
                    .hasMessageContaining("do not own this order");
        }

        @Test
        void getOrderById_throwsException_whenNotFound() {
            when(orderRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> orderService.getOrderById(999L))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    class GetMyOrders {

        @Test
        void getMyOrders_returnsOrders() {
            when(orderRepository.findByCustomerUserEmail("customer@example.com")).thenReturn(List.of(order));
            when(orderItemRepository.findByOrderOrderId(1000L)).thenReturn(List.of(buildSavedOrderItem()));

            List<OrderResponse> responses = orderService.getMyOrders();
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).orderId()).isEqualTo(1000L);
        }

        @Test
        void getMyOrders_returnsEmptyList() {
            when(orderRepository.findByCustomerUserEmail("customer@example.com")).thenReturn(Collections.emptyList());
            assertThat(orderService.getMyOrders()).isEmpty();
        }
    }

    @Nested
    class GetAllOrders {

        @Test
        void getAllOrders_returnsAllOrders() {
            when(orderRepository.findAll()).thenReturn(List.of(order));
            when(orderItemRepository.findByOrderOrderId(1000L)).thenReturn(List.of(buildSavedOrderItem()));

            List<OrderResponse> responses = orderService.getAllOrders();
            assertThat(responses).hasSize(1);
        }

        @Test
        void getAllOrders_returnsEmptyList() {
            when(orderRepository.findAll()).thenReturn(Collections.emptyList());
            assertThat(orderService.getAllOrders()).isEmpty();
        }
    }

    @Nested
    class GetAllOrdersPaginated {

        @Test
        void getAllOrdersPaginated_returnsPaginatedOrders() {
            PageRequest pageable = PageRequest.of(0, 10);
            when(orderRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(order), pageable, 1));
            when(orderItemRepository.findByOrderOrderId(1000L)).thenReturn(List.of(buildSavedOrderItem()));

            Page<OrderResponse> result = orderService.getAllOrdersPaginated(pageable);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).orderId()).isEqualTo(1000L);
        }
    }

    @Nested
    class UpdateOrderStatus {

        @Test
        void updateOrderStatus_updatesAndPublishesEvent() {
            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(Order.OrderStatus.PAID, PaymentStatus.COMPLETED);
            when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderItemRepository.findByOrderOrderId(1000L)).thenReturn(List.of(buildSavedOrderItem()));

            OrderResponse response = orderService.updateOrderStatus(1000L, request);

            assertThat(response.orderStatus()).isEqualTo(Order.OrderStatus.PAID);
            assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);

            ArgumentCaptor<OrderStatusUpdateEvent> captor = ArgumentCaptor.forClass(OrderStatusUpdateEvent.class);
            verify(orderEventPublisher).publishOrderStatusUpdated(captor.capture());
            OrderStatusUpdateEvent event = captor.getValue();
            assertThat(event.orderId()).isEqualTo(1000L);
            assertThat(event.previousStatus()).isEqualTo(Order.OrderStatus.PENDING);
            assertThat(event.newStatus()).isEqualTo(Order.OrderStatus.PAID);
            assertThat(event.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(event.customerEmail()).isEqualTo("customer@example.com");
        }

        @Test
        void updateOrderStatus_keepsPaymentStatus_whenNull() {
            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(Order.OrderStatus.SHIPPED, null);
            when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderItemRepository.findByOrderOrderId(1000L)).thenReturn(List.of(buildSavedOrderItem()));

            OrderResponse response = orderService.updateOrderStatus(1000L, request);
            assertThat(response.orderStatus()).isEqualTo(Order.OrderStatus.SHIPPED);
            assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        void updateOrderStatus_throwsException_whenNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> orderService.updateOrderStatus(999L, new OrderStatusUpdateRequest(Order.OrderStatus.PAID, null)))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    class CancelOrder {

        @Test
        void cancelOrder_cancelsOrder_andRestoresStock() {
            OrderItem orderItem = buildSavedOrderItem();
            when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
            when(orderItemRepository.findByOrderOrderId(1000L)).thenReturn(List.of(orderItem));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            when(productRepository.save(productCaptor.capture())).thenReturn(product);

            OrderResponse response = orderService.cancelOrder(1000L);

            assertThat(response.orderStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
            assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(productCaptor.getValue().getQuantity()).isEqualTo(7); // 5 + 2

            ArgumentCaptor<OrderStatusUpdateEvent> eventCaptor = ArgumentCaptor.forClass(OrderStatusUpdateEvent.class);
            verify(orderEventPublisher).publishOrderStatusUpdated(eventCaptor.capture());
            assertThat(eventCaptor.getValue().newStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        }

        @Test
        void cancelOrder_restoresAvailability_whenWasOutOfStock() {
            product.setQuantity(0);
            product.setStatus(Product.Status.OUT_OF_STOCK);

            when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
            when(orderItemRepository.findByOrderOrderId(1000L)).thenReturn(List.of(buildSavedOrderItem()));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            when(productRepository.save(captor.capture())).thenReturn(product);

            orderService.cancelOrder(1000L);

            assertThat(captor.getValue().getStatus()).isEqualTo(Product.Status.AVAILABLE);
            assertThat(captor.getValue().getQuantity()).isEqualTo(2);
        }

        @Test
        void cancelOrder_throwsException_whenAlreadyCancelled() {
            order.setOrderStatus(Order.OrderStatus.CANCELLED);
            when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1000L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already cancelled");
        }

        @Test
        void cancelOrder_throwsException_whenShipped() {
            order.setOrderStatus(Order.OrderStatus.SHIPPED);
            when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1000L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel");
        }

        @Test
        void cancelOrder_throwsException_whenDelivered() {
            order.setOrderStatus(Order.OrderStatus.DELIVERED);
            when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1000L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel");
        }

        @Test
        void cancelOrder_throwsException_whenNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> orderService.cancelOrder(999L))
                    .isInstanceOf(OrderNotFoundException.class);
        }

        @Test
        void cancelOrder_throwsException_whenNonOwnerCancels() {
            setAuthenticatedUser("thief@example.com", "ROLE_CUSTOMER");
            when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelOrder(1000L))
                    .isInstanceOf(UnauthorizedOrderAccessException.class);
        }

        @Test
        void cancelOrder_allowsAdmin_toCancelAnyOrder() {
            setAuthenticatedUser("admin@example.com", "ROLE_ADMIN");
            when(orderRepository.findById(1000L)).thenReturn(Optional.of(order));
            when(orderItemRepository.findByOrderOrderId(1000L)).thenReturn(List.of(buildSavedOrderItem()));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.save(any(Product.class))).thenReturn(product);

            assertThat(orderService.cancelOrder(1000L).orderStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        }
    }
}

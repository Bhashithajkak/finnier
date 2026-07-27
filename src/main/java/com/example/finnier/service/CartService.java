package com.example.finnier.service;

import com.example.finnier.dto.CartItemRequest;
import com.example.finnier.dto.CartItemResponse;
import com.example.finnier.dto.CartResponse;
import com.example.finnier.entity.Cart;
import com.example.finnier.entity.CartItem;
import com.example.finnier.entity.Customer;
import com.example.finnier.entity.Product;
import com.example.finnier.repository.CartItemRepository;
import com.example.finnier.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerService customerService;
    private final ProductService productService;


    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, CustomerService customerService, ProductService productService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.customerService = customerService;
        this.productService = productService;
    }

    public Cart createCart(Long customerId) {

        Customer customer = customerService.findCustomerById(customerId);

        return cartRepository.findByCustomerCustomerId(customerId)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setCustomer(customer);
                    return cartRepository.save(cart);
                });
    }

    public Cart getCartByCustomerId(Long customerId) {

        return cartRepository.findByCustomerCustomerId(customerId)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found for customer id : " + customerId));
    }

    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    public void deleteCart(Long cartId) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() ->
                        new RuntimeException("Cart not found"));

        cartRepository.delete(cart);
    }

    public List<CartItemResponse> getCartItems(Long cartId){
        return cartItemRepository.findAllByCartCartId(cartId)
                .stream()
                .map(this::mapToCartItemResponse)
                .toList();
    }
    @Transactional
    public CartResponse addItemToCart(CartItemRequest request){
        Cart cart = getCartByCustomerId(request.customerId());
        Product product = productService.getProductEntityById(request.productId());
        cartItemRepository.save(CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(request.quantity())
                .subtotal(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())))
                .build());
        return mapToCartResponse(cart, getCartItems(cart.getCartId()));

    }

    public void removeCartItem(Long itemId){
        cartItemRepository.findById(itemId).orElseThrow(
                ()-> new RuntimeException("Cart item not found with id: " + itemId)
        );
    }

    @Transactional
    public CartResponse updateCartItemQty(CartItemRequest request){
        Cart cart = getCartByCustomerId(request.customerId());
        Product product = productService.getProductEntityById(request.productId());
        cartItemRepository.save(CartItem.builder()
                .cartItemId(request.cartItemId())
                .cart(cart)
                .product(product)
                .quantity(request.quantity())
                .subtotal(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())))
                .build());
        return mapToCartResponse(cart, getCartItems(cart.getCartId()));
    }

    private CartResponse mapToCartResponse(Cart cart, List<CartItemResponse> items){
        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        return new CartResponse(
                cart.getCartId(),
                cart.getCustomer().getCustomerId(),
                items,
                total,
                cart.getUpdatedAt()
        );
    }
    private CartItemResponse mapToCartItemResponse(CartItem cartItem){
        return new CartItemResponse(
                cartItem.getCartItemId(),
                cartItem.getProduct().getProductId(),
                cartItem.getProduct().getName(),
                cartItem.getQuantity(),
                cartItem.getSubtotal()
        );
    }


}
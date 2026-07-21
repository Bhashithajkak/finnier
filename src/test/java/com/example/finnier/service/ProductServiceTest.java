package com.example.finnier.service;

import com.example.finnier.dto.ProductRequestDto;
import com.example.finnier.dto.ProductResponseDto;
import com.example.finnier.entity.Category;
import com.example.finnier.entity.Product;
import com.example.finnier.repository.CategoryRepository;
import com.example.finnier.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private Category category;
    private ProductRequestDto productRequestDto;

    @BeforeEach
    void setUp(){
        category = Category.builder()
                .categoryId(1L)
                .categoryName("Electronics")
                .build();

        product = Product.builder()
                .productId(1L)
                .category(category)
                .name("Laptop")
                .description("Gaming Laptop")
                .price(new BigDecimal("1500.00"))
                .quantity(10)
                .status(Product.Status.AVAILABLE)
                .build();
        product.setCreatedAt(LocalDateTime.now());

        productRequestDto = new ProductRequestDto(
                1L,
                "Laptop",
                "Gaming Laptop",
                new BigDecimal("1500.00"),
                10,
                Product.Status.AVAILABLE
        );
    }

    @Test
    void createProduct_ShouldReturnSavedProduct() {
        when (categoryRepository.findById(productRequestDto.categoryId()))
                .thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponseDto responseDto = productService.createProduct(productRequestDto);

        assertNotNull(responseDto);
        assertEquals(product.getProductId(), responseDto.productId());
        assertEquals(product.getCategory().getCategoryId(), responseDto.categoryId());
        assertEquals(product.getName(), responseDto.name());
        assertEquals(product.getDescription(), responseDto.description());
        assertEquals(product.getPrice(), responseDto.price());
        assertEquals(product.getQuantity(), responseDto.quantity());
        assertEquals(product.getStatus(), responseDto.status());

        verify(categoryRepository).findById(productRequestDto.categoryId());
        verify(productRepository).save(any(Product.class));
    }
    @Test
    void getAllProducts_ShouldReturnListOfProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(product)));

        List<ProductResponseDto> products = productService.getAllProducts(PageRequest.of(0, 10));

        assertEquals(1, products.size());
        assertEquals(product.getProductId(), products.get(0).productId());

        verify(productRepository).findAll(pageable);
    }

    @Test
    void getProductById_ShouldReturnProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponseDto responseDto = productService.getProductById(1L);

        assertNotNull(responseDto);
        assertEquals(product.getProductId(), responseDto.productId());

        verify(productRepository).findById(1L);
    }

    @Test
    void getProductById_ShouldThrowException_WhenProductNotFound(){
        when(productRepository.findById(2L)).thenReturn(Optional.empty());

        try {
            productService.getProductById(2L);
        } catch (Exception e) {
            assertEquals("Product not found with id: 2", e.getMessage());
        }

        verify(productRepository).findById(2L);
    }

    @Test
    void updateProduct_ShouldUpdateAndReturnProduct(){
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));
        when(productRepository.save(product))
                .thenReturn(product);

        ProductResponseDto responseDto = productService.updateProduct(productRequestDto, 1L);

        assertNotNull(responseDto);
        assertEquals(product.getProductId(), responseDto.productId());
        assertEquals(product.getCategory().getCategoryId(), responseDto.categoryId());

        verify(productRepository).findById(1L);
        verify(productRepository).save(product);
        verify(categoryRepository,never()).findById(anyLong());
    }

    @Test
    void updateProduct_ShouldChangeCategoryWhenDifferent(){
        Category newCategory = Category.builder()
                .categoryId(2L)
                .categoryName("Home Appliances")
                .build();

        ProductRequestDto updateRequest = new ProductRequestDto(
                newCategory.getCategoryId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getStatus()
        );

        when(productRepository.findById(product.getProductId()))
                .thenReturn(Optional.of(product));
        when(categoryRepository.findById(newCategory.getCategoryId()))
                .thenReturn(Optional.of(newCategory));
        when(productRepository.save(product))
                .thenReturn(product);
        ProductResponseDto responseDto = productService.updateProduct(updateRequest, product.getProductId());

        assertNotNull(responseDto);
        assertEquals(responseDto.categoryId(),newCategory.getCategoryId());
        assertEquals(responseDto.categoryName(),newCategory.getCategoryName());

        verify(productRepository).findById(product.getProductId());
        verify(categoryRepository).findById(newCategory.getCategoryId());
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_shouldThrowExceptionProductNotFound(){
        when(productRepository.findById(2L))
                .thenReturn(Optional.empty());
        assertThrows(
                EntityNotFoundException.class,
                () -> productService.updateProduct(productRequestDto, 2L)
        );

        verify(productRepository).findById(2L);
    }

    @Test
    void deleteProduct_shouldDeleteProduct(){
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        productService.deleteProduct(1L);

        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_shouldThrowExceptionWhenNotFound(){
        when(productRepository.existsById(2L)).thenReturn(false);

        assertThrows(
                EntityNotFoundException.class,
                () -> productService.deleteProduct(2L)
        );

        verify(productRepository).existsById(2L);
        verify(productRepository, never()).deleteById(anyLong());
    }

}

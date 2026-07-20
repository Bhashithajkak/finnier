package com.example.finnier.service;

import com.example.finnier.dto.ProductRequestDto;
import com.example.finnier.dto.ProductResponseDto;
import com.example.finnier.entity.Category;
import com.example.finnier.entity.Product;
import com.example.finnier.repository.CategoryRepository;
import com.example.finnier.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductResponseDto createProduct(ProductRequestDto createProductDto) {
        return toResponseDto(productRepository.save(toProduct(createProductDto)));
    }

    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public ProductResponseDto getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Product not found with id: " + productId
                ));
        return toResponseDto(product);
    }

    @Transactional
    public ProductResponseDto updateProduct(ProductRequestDto updateProductDto, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Product not found with id: " + productId
                ));

        if (!updateProductDto.categoryId().equals(product.getCategory().getCategoryId())) {
            Category newCategory = categoryRepository.findById(updateProductDto.categoryId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Category not found with id: " + updateProductDto.categoryId()
                    ));
            product.setCategory(newCategory);
        }

        product.setName(updateProductDto.name());
        product.setDescription(updateProductDto.description());
        product.setPrice(updateProductDto.price());
        product.setQuantity(updateProductDto.quantity());
        product.setStatus(updateProductDto.status());

        return toResponseDto(productRepository.save(product));
    }

    public void deleteProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("Product not found with id: " + productId);
        }
        productRepository.deleteById(productId);
    }

    private Product toProduct(ProductRequestDto productDto) {
        Category productCategory = categoryRepository.findById(productDto.categoryId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category not found with id: " + productDto.categoryId()
                ));
        return Product.builder()
                .category(productCategory)
                .name(productDto.name())
                .description(productDto.description())
                .price(productDto.price())
                .quantity(productDto.quantity())
                .status(productDto.status())
                .build();
    }

    private ProductResponseDto toResponseDto(Product product) {
        return new ProductResponseDto(
                product.getProductId(),
                product.getCategory().getCategoryId(),
                product.getCategory().getCategoryName(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getStatus(),
                product.getCreatedAt()
        );
    }
}

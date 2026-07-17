package com.example.finnier.service;

import com.example.finnier.dto.ProductRequestDto;
import com.example.finnier.dto.ProductResponseDto;
import com.example.finnier.entity.Category;
import com.example.finnier.entity.Product;
import com.example.finnier.repository.CategoryRepository;
import com.example.finnier.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductResponseDto createProduct(ProductRequestDto createProductDto){
        return toResponseDto(productRepository.save(toProduct(createProductDto)));
    }

    private Product toProduct(ProductRequestDto productDto){
        Category productCategory = categoryRepository.findById(productDto.categoryId())
                .orElseThrow(()-> new EntityNotFoundException(
                        "Category not found with id: "+productDto.categoryId()
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

package com.example.finnier.service;

import com.example.finnier.dto.CategoryRequestDto;
import com.example.finnier.dto.CategoryResponseDto;
import com.example.finnier.entity.Category;
import com.example.finnier.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository){
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponseDto createCategory(CategoryRequestDto categoryRequestDto) {
        Category category = categoryRepository.save(
                Category.builder()
                        .categoryName(categoryRequestDto.categoryName())
                        .build()
        );
        return toResponseDto(category);
    }

    public List<CategoryResponseDto> getAllCategories(){
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }
    public CategoryResponseDto getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .map(this::toResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));
    }

    public CategoryResponseDto updateCategory(CategoryRequestDto categoryRequestDto, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));
        category.setCategoryName(categoryRequestDto.categoryName());
        return toResponseDto(categoryRepository.save(category));
    }
    public void deleteCategory(Long categoryId) {
        if(!categoryRepository.existsById(categoryId)) {
                throw new EntityNotFoundException("Category not found with id: " + categoryId);
        }
        categoryRepository.deleteById(categoryId);
    }
    private CategoryResponseDto toResponseDto(Category category) {
        return new CategoryResponseDto(
                category.getCategoryId(),
                category.getCategoryName()
        );
    }

}

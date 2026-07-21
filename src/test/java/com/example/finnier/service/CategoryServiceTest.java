package com.example.finnier.service;

import com.example.finnier.dto.CategoryRequestDto;
import com.example.finnier.dto.CategoryResponseDto;
import com.example.finnier.entity.Category;
import com.example.finnier.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    private CategoryService categoryService;
    private Category category;
    private CategoryRequestDto requestDto;


    @BeforeEach
    void setUp() {

        category = Category.builder()
                .categoryId(1L)
                .categoryName("Electronics")
                .build();


        requestDto = new CategoryRequestDto(
                "Electronics"
        );
    }


    @Test
    void createCategory_ShouldReturnCategoryResponseDto() {

        when(categoryRepository.save(any(Category.class)))
                .thenReturn(category);


        CategoryResponseDto response =
                categoryService.createCategory(requestDto);


        assertNotNull(response);
        assertEquals(category.getCategoryId(), response.categoryId());
        assertEquals(category.getCategoryName(), response.categoryName());


        verify(categoryRepository).save(any(Category.class));
    }


    @Test
    void createCategory_ShouldSaveCorrectCategory() {

        when(categoryRepository.save(any(Category.class)))
                .thenReturn(category);


        categoryService.createCategory(requestDto);


        ArgumentCaptor<Category> captor =
                ArgumentCaptor.forClass(Category.class);


        verify(categoryRepository)
                .save(captor.capture());


        Category savedCategory = captor.getValue();


        assertEquals(
                requestDto.categoryName(),
                savedCategory.getCategoryName()
        );
    }


    @Test
    void getAllCategories_ShouldReturnCategoryList() {

        when(categoryRepository.findAll())
                .thenReturn(List.of(category));

        List<CategoryResponseDto> response =
                categoryService.getAllCategories();


        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(
                category.getCategoryName(),
                response.get(0).categoryName()
        );

        verify(categoryRepository).findAll();
    }
    @Test
    void getCategoryById_ShouldReturnCategory() {

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        CategoryResponseDto response =
                categoryService.getCategoryById(1L);

        assertNotNull(response);
        assertEquals(
                category.getCategoryId(),
                response.categoryId()
        );
        assertEquals(
                category.getCategoryName(),
                response.categoryName()
        );

        verify(categoryRepository).findById(1L);
    }
    @Test
    void getCategoryById_ShouldThrowExceptionWhenNotFound() {

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.getCategoryById(1L)
        );

        verify(categoryRepository).findById(1L);
    }
    @Test
    void updateCategory_ShouldUpdateCategoryName() {

        CategoryRequestDto updateRequest = new CategoryRequestDto("Home Appliances");

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(categoryRepository.save(category))
                .thenReturn(category);

        CategoryResponseDto response = categoryService.updateCategory(updateRequest,1L);
        assertNotNull(response);
        assertEquals("Home Appliances", response.categoryName());

        verify(categoryRepository).findById(1L);
        verify(categoryRepository).save(category);
    }


    @Test
    void updateCategory_ShouldThrowExceptionWhenCategoryNotFound() {

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.updateCategory(requestDto, 1L)
        );

        verify(categoryRepository).findById(1L);
        verify(categoryRepository, never())
                .save(any(Category.class));
    }


    @Test
    void deleteCategory_ShouldDeleteCategory() {

        when(categoryRepository.existsById(1L))
                .thenReturn(true);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).existsById(1L);
        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteCategory_ShouldThrowExceptionWhenCategoryNotFound() {

        when(categoryRepository.existsById(1L))
                .thenReturn(false);

        assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.deleteCategory(1L)
        );

        verify(categoryRepository).existsById(1L);

        verify(categoryRepository, never()).deleteById(anyLong());
    }
}
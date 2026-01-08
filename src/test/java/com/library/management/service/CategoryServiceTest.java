package com.library.management.service;

import com.library.management.entity.Category;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ResourceNotFoundException;
import com.library.management.repository.CategoryRepository;
import com.library.management.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Fiction");
        testCategory.setDescription("Fiction books");
        testCategory.setStatus(Category.Status.ACTIVE);
    }

    @Test
    @DisplayName("Should create category successfully")
    void testCreateCategory_Success() {
        // Given
        when(categoryRepository.existsByName(testCategory.getName())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // When
        Category result = categoryService.createCategory(testCategory);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Fiction");
        assertThat(result.getDescription()).isEqualTo("Fiction books");
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw exception when creating category with duplicate name")
    void testCreateCategory_DuplicateName() {
        // Given
        when(categoryRepository.existsByName(testCategory.getName())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> categoryService.createCategory(testCategory))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Should get category by ID successfully")
    void testGetCategoryById_Success() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // When
        Category result = categoryService.getCategoryById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Fiction");
        verify(categoryRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when category not found by ID")
    void testGetCategoryById_NotFound() {
        // Given
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    @DisplayName("Should get all categories successfully")
    void testGetAllCategories_Success() {
        // Given
        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("Science");
        category2.setStatus(Category.Status.ACTIVE);

        List<Category> categories = Arrays.asList(testCategory, category2);
        when(categoryRepository.findAll()).thenReturn(categories);

        // When
        List<Category> result = categoryService.getAllCategories();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).contains(testCategory, category2);
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should update category successfully")
    void testUpdateCategory_Success() {
        // Given
        Category updatedCategory = new Category();
        updatedCategory.setName("Science Fiction");
        updatedCategory.setDescription("Science Fiction books");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByName(updatedCategory.getName())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // When
        Category result = categoryService.updateCategory(1L, updatedCategory);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Science Fiction");
        assertThat(result.getDescription()).isEqualTo("Science Fiction books");
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent category")
    void testUpdateCategory_NotFound() {
        // Given
        Category updatedCategory = new Category();
        updatedCategory.setName("Updated");

        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> categoryService.updateCategory(99L, updatedCategory))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    @DisplayName("Should throw exception when updating with duplicate name")
    void testUpdateCategory_DuplicateName() {
        // Given
        Category updatedCategory = new Category();
        updatedCategory.setName("Science");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByName("Science")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> categoryService.updateCategory(1L, updatedCategory))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should delete category successfully")
    void testDeleteCategory_Success() {
        // Given
        when(categoryRepository.existsById(1L)).thenReturn(true);
        doNothing().when(categoryRepository).deleteById(1L);

        // When
        categoryService.deleteCategory(1L);

        // Then
        verify(categoryRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent category")
    void testDeleteCategory_NotFound() {
        // Given
        when(categoryRepository.existsById(anyLong())).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
        verify(categoryRepository, never()).deleteById(anyLong());
    }
}

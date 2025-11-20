package com.selimhorri.app.unit.service;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.service.impl.CategoryServiceImpl;
import com.selimhorri.app.unit.util.CategoryUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private CategoryDto category;

    @BeforeEach
    void setUp() {
        category =  CategoryUtil.getSampleCategoryDto();
    }

    @Test
    void testFindById_ShouldReturnCategoryDto() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(CategoryUtil.getSampleCategory()));

        CategoryDto result = categoryService.findById(category.getCategoryId());

        assertNotNull(result);
        assertEquals(category.getCategoryId(), result.getCategoryId());
        assertEquals(category.getCategoryTitle(), result.getCategoryTitle());
        assertEquals(category.getImageUrl(), result.getImageUrl());
    }

    @Test
    void testSave_ShouldReturnSavedCategoryDto() {
        when(categoryRepository.save(any(Category.class))).thenReturn(CategoryUtil.getSampleCategory());

        CategoryDto result = categoryService.save(category);

        assertNotNull(result);
        assertEquals(category.getCategoryId(), result.getCategoryId());
        assertEquals(category.getCategoryTitle(), result.getCategoryTitle());
        assertEquals(category.getImageUrl(), result.getImageUrl());
    }
}




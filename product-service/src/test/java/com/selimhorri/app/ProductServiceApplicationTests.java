package com.selimhorri.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.impl.ProductServiceImpl;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

	private static final Integer PRODUCT_ID = 100;
	private static final Integer CATEGORY_ID = 5;
	private static final String SKU = "SKU-001";

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private ProductServiceImpl productService;

	private Product productEntity;
	private ProductDto productDto;

	@BeforeEach
	void setUp() {
		productEntity = buildProductEntity();
		productDto = buildProductDto();
	}

	@Test
	void findAll_whenProductsExist_returnsMappedDtos() {
		when(productRepository.findAll()).thenReturn(List.of(productEntity));

		List<ProductDto> result = productService.findAll();

		assertEquals(1, result.size());
		assertEquals(SKU, result.get(0).getSku());
		verify(productRepository, times(1)).findAll();
	}

	@Test
	void findById_whenProductExists_returnsDto() {
		when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productEntity));

		ProductDto result = productService.findById(PRODUCT_ID);

		assertEquals(PRODUCT_ID, result.getProductId());
		assertEquals("Gaming Laptop", result.getProductTitle());
	}

	@Test
	void findById_whenProductMissing_throwsException() {
		when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

		assertThrows(ProductNotFoundException.class, () -> productService.findById(PRODUCT_ID));
	}

	@Test
	void save_whenProductValid_persistsAndReturnsMappedProduct() {
		when(productRepository.save(any(Product.class))).thenReturn(productEntity);

		ProductDto result = productService.save(productDto);

		assertNotNull(result);
		assertEquals(PRODUCT_ID, result.getProductId());

		ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
		verify(productRepository, times(1)).save(productCaptor.capture());
		Product savedEntity = productCaptor.getValue();
		assertEquals(SKU, savedEntity.getSku());
		assertEquals(CATEGORY_ID, savedEntity.getCategory().getCategoryId());
	}

	@Test
	void update_whenProductValid_persistsAndReturnsMappedProduct() {
		when(productRepository.save(any(Product.class))).thenReturn(productEntity);

		ProductDto result = productService.update(productDto);

		assertEquals(PRODUCT_ID, result.getProductId());
		verify(productRepository, times(1)).save(any(Product.class));
	}

	@Test
	void updateById_whenProductExists_checksPresenceAndSaves() {
		when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productEntity));
		when(productRepository.save(any(Product.class))).thenReturn(productEntity);

		ProductDto result = productService.update(PRODUCT_ID, productDto);

		assertEquals(PRODUCT_ID, result.getProductId());
		verify(productRepository, times(1)).findById(PRODUCT_ID);
		verify(productRepository, times(1)).save(any(Product.class));
	}

	@Test
	void deleteById_always_fetchesThenDeletesMappedEntity() {
		when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productEntity));

		productService.deleteById(PRODUCT_ID);

		verify(productRepository, times(1)).findById(PRODUCT_ID);
		verify(productRepository, times(1)).delete(any(Product.class));
	}

	private Product buildProductEntity() {
		Category category = Category.builder()
			.categoryId(CATEGORY_ID)
			.categoryTitle("Electronics")
			.imageUrl("/img/electronics.png")
			.build();

		return Product.builder()
			.productId(PRODUCT_ID)
			.productTitle("Gaming Laptop")
			.imageUrl("/img/laptop.png")
			.sku(SKU)
			.priceUnit(1999.99)
			.quantity(5)
			.category(category)
			.build();
	}

	private ProductDto buildProductDto() {
		CategoryDto categoryDto = CategoryDto.builder()
			.categoryId(CATEGORY_ID)
			.categoryTitle("Electronics")
			.imageUrl("/img/electronics.png")
			.build();

		return ProductDto.builder()
			.productId(PRODUCT_ID)
			.productTitle("Gaming Laptop")
			.imageUrl("/img/laptop.png")
			.sku(SKU)
			.priceUnit(1999.99)
			.quantity(5)
			.categoryDto(categoryDto)
			.build();
	}
}







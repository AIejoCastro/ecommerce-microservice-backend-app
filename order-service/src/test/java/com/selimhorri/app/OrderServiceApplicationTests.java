package com.selimhorri.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.OrderService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceIntegrationTest {

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private CartRepository cartRepository;

	private Cart persistedCart;

	@BeforeEach
	void setUp() {
		orderRepository.deleteAll();
		cartRepository.deleteAll();
		persistedCart = cartRepository.save(Cart.builder()
			.userId(777)
			.build());
	}

	@Test
	void save_whenOrderValid_persistsAndReturnsDto() {
		OrderDto toSave = buildOrderDto(null, "Launch order");

		OrderDto saved = orderService.save(toSave);

		assertNotNull(saved.getOrderId());
		assertEquals(toSave.getOrderDesc(), saved.getOrderDesc());
		assertThat(orderRepository.findById(saved.getOrderId())).isPresent();
	}

	@Test
	void update_whenOrderExists_updatesAndReturnsDto() {
		OrderDto initial = orderService.save(buildOrderDto(null, "Original order"));
		OrderDto toUpdate = OrderDto.builder()
			.orderId(initial.getOrderId())
			.orderDate(initial.getOrderDate())
			.orderDesc("Updated order")
			.orderFee(150.75)
			.cartDto(initial.getCartDto())
			.build();

		OrderDto updated = orderService.update(toUpdate);

		assertEquals(initial.getOrderId(), updated.getOrderId());
		assertEquals("Updated order", updated.getOrderDesc());
		assertThat(orderRepository.findById(updated.getOrderId()))
			.get()
			.extracting(order -> order.getOrderFee())
			.isEqualTo(150.75);
	}

	@Test
	void findById_whenOrderMissing_throwsOrderNotFound() {
		assertThrows(OrderNotFoundException.class, () -> orderService.findById(9999));
	}

	@Test
	void deleteById_whenOrderExists_removesItFromRepository() {
		OrderDto saved = orderService.save(buildOrderDto(null, "Disposable order"));

		orderService.deleteById(saved.getOrderId());

		assertThat(orderRepository.findById(saved.getOrderId())).isEmpty();
	}

	private OrderDto buildOrderDto(Integer orderId, String description) {
		return OrderDto.builder()
			.orderId(orderId)
			.orderDate(LocalDateTime.of(2024, 1, 1, 10, 30, 0, 123000000))
			.orderDesc(description)
			.orderFee(99.99)
			.cartDto(CartDto.builder()
				.cartId(persistedCart.getCartId())
				.userId(persistedCart.getUserId())
				.build())
			.build();
	}
}







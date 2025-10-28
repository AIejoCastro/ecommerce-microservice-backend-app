package com.selimhorri.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderResourceE2ETest {

    private static final String BASE_PATH = "/api/orders";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Cart cart;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        cart = cartRepository.save(Cart.builder()
            .userId(42)
            .build());
    }

    @Test
    void endToEnd_createReadDeleteOrder_flowSucceeds() throws Exception {
        OrderDto orderRequest = buildOrderDto(null, "E2E order");

        ResponseEntity<String> createResponse = restTemplate.postForEntity(BASE_PATH, orderRequest, String.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody()).isNotNull();
        OrderDto createdOrder = objectMapper.readValue(createResponse.getBody(), OrderDto.class);
        Integer orderId = createdOrder.getOrderId();
        assertThat(orderId).isNotNull();

        ResponseEntity<String> fetchResponse = restTemplate.getForEntity(BASE_PATH + "/" + orderId, String.class);
        assertThat(fetchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetchResponse.getBody()).isNotNull();
        OrderDto fetchedOrder = objectMapper.readValue(fetchResponse.getBody(), OrderDto.class);
        assertThat(fetchedOrder.getOrderDesc()).isEqualTo("E2E order");

        ResponseEntity<Boolean> deleteResponse = restTemplate.exchange(BASE_PATH + "/" + orderId, HttpMethod.DELETE, HttpEntity.EMPTY, Boolean.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleteResponse.getBody()).isTrue();

        ResponseEntity<String> deletedFetchResponse = restTemplate.getForEntity(BASE_PATH + "/" + orderId, String.class);
        assertThat(deletedFetchResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(deletedFetchResponse.getBody()).isNotBlank();
        JsonNode deletedFetchMessage = objectMapper.readTree(deletedFetchResponse.getBody());
        assertThat(deletedFetchMessage.get("msg").asText()).contains(String.valueOf(orderId));
    }

    @Test
    void getOrder_whenDoesNotExist_returnsBadRequest() {
        ResponseEntity<String> response = restTemplate.getForEntity(BASE_PATH + "/99999", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private OrderDto buildOrderDto(Integer orderId, String description) {
        return OrderDto.builder()
            .orderId(orderId)
            .orderDate(LocalDateTime.of(2024, 1, 2, 9, 0, 0, 0))
            .orderDesc(description)
            .orderFee(59.99)
            .cartDto(CartDto.builder()
                .cartId(cart.getCartId())
                .userId(cart.getUserId())
                .build())
            .build();
    }
}

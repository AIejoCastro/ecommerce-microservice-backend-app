package com.selimhorri.app.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.selimhorri.app.config.TestRestTemplateConfig.class)
class OrderServiceEndToEndIT {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(AppConstant.LOCAL_DATE_TIME_FORMAT);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanData() {
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        mockServer.reset();
    }

    @Test
    @DisplayName("Flujo E2E: crear un carrito y recuperar usuario desde User Service")
    void createCartAndFetchUser() throws Exception {
    String createPayload = "{\"userId\":123}";

        String cartResponse = mockMvc.perform(post("/api/carts")
                .contentType(APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Integer cartId = JsonPath.read(cartResponse, "$.cartId");

        UserDto remoteUser = UserDto.builder()
            .userId(123)
            .firstName("End")
            .lastName("ToEnd")
            .email("endtoend@example.com")
            .build();

        mockServer.expect(requestTo(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/123"))
            .andRespond(withSuccess(objectMapper.writeValueAsString(remoteUser), APPLICATION_JSON));

        mockMvc.perform(get("/api/carts/" + cartId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.userId").value(123))
            .andExpect(jsonPath("$.user.email").value("endtoend@example.com"));
    }

    @Test
    @DisplayName("Flujo E2E: crear una orden asociada a un carrito")
    void createOrderForCart() throws Exception {
        Cart cart = cartRepository.save(Cart.builder().userId(555).build());

    String orderPayload = String.format("{\"orderDate\": \"%s\", \"orderDesc\": \"checkout\", \"orderFee\": 99.9, \"cart\": {\"cartId\": %d}}",
            LocalDateTime.now().format(FORMATTER), cart.getCartId());

        mockMvc.perform(post("/api/orders")
                .contentType(APPLICATION_JSON)
                .content(orderPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").isNumber())
            .andExpect(jsonPath("$.orderDesc").value("checkout"));

        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Flujo E2E: actualizar el usuario asociado al carrito")
    void updateCartUser() throws Exception {
        Cart cart = cartRepository.save(Cart.builder().userId(41).build());

    String updatePayload = String.format("{\"cartId\": %d, \"userId\": 42}", cart.getCartId());

        mockMvc.perform(put("/api/carts")
                .contentType(APPLICATION_JSON)
                .content(updatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(42));

        assertThat(cartRepository.findById(cart.getCartId())).get().extracting(Cart::getUserId).isEqualTo(42);
    }

    @Test
    @DisplayName("Flujo E2E: eliminar un carrito y validar limpieza")
    void deleteCartFlow() throws Exception {
        Cart cart = cartRepository.save(Cart.builder().userId(90).build());

        mockMvc.perform(delete("/api/carts/" + cart.getCartId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(true));

        assertThat(cartRepository.existsById(cart.getCartId())).isFalse();
    }

    @Test
    @DisplayName("Flujo E2E: listar órdenes registradas")
    void listOrdersFlow() throws Exception {
        Cart cart = cartRepository.save(Cart.builder().userId(61).build());
        orderRepository.save(Order.builder()
            .orderDate(LocalDateTime.now())
            .orderDesc("order-one")
            .orderFee(15.0)
            .cart(cart)
            .build());
        orderRepository.save(Order.builder()
            .orderDate(LocalDateTime.now())
            .orderDesc("order-two")
            .orderFee(20.0)
            .cart(cart)
            .build());

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(2));
    }
}

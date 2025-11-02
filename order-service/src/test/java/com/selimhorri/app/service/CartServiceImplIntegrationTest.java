package com.selimhorri.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.CartRepository;

@SpringBootTest
@org.springframework.context.annotation.Import(com.selimhorri.app.config.TestRestTemplateConfig.class)
@ActiveProfiles("test")
class CartServiceImplIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    private MockRestServiceServer mockServer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("restTemplateBean")
    private RestTemplate restTemplate;

    @BeforeEach
    void cleanDatabase() {
        cartRepository.deleteAll();
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    @DisplayName("findById enriquece el carrito con información del User Service")
    void findByIdEnrichesCartWithRemoteUser() throws JsonProcessingException {
        Cart persisted = cartRepository.save(Cart.builder().userId(5).build());

        UserDto remoteUser = UserDto.builder()
            .userId(5)
            .firstName("Integration")
            .lastName("Test")
            .email("integration@test.com")
            .build();

        mockServer.expect(requestTo(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + persisted.getUserId()))
            .andRespond(withSuccess(objectMapper.writeValueAsString(remoteUser), MediaType.APPLICATION_JSON));

        CartDto dto = cartService.findById(persisted.getCartId());

        assertThat(dto.getUserDto().getEmail()).isEqualTo("integration@test.com");
        assertThat(dto.getUserDto().getUserId()).isEqualTo(persisted.getUserId());
    }

    @Test
    @DisplayName("findAll obtiene todos los carritos y resuelve los usuarios remotos")
    void findAllResolvesRemoteUsers() throws JsonProcessingException {
        List<Cart> carts = cartRepository.saveAll(List.of(
            Cart.builder().userId(10).build(),
            Cart.builder().userId(11).build()
        ));

        for (Cart cart : carts) {
            UserDto user = UserDto.builder()
                .userId(cart.getUserId())
                .firstName("User" + cart.getUserId())
                .email("user" + cart.getUserId() + "@test.com")
                .build();

            mockServer.expect(requestTo(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + cart.getUserId()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(user), MediaType.APPLICATION_JSON));
        }

        List<CartDto> dtos = cartService.findAll();

        assertThat(dtos)
            .hasSize(2)
            .allSatisfy(dto -> assertThat(dto.getUserDto().getEmail()).endsWith("@test.com"));
    }
}

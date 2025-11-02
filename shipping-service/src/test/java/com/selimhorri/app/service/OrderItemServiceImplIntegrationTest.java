package com.selimhorri.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.repository.OrderItemRepository;

@SpringBootTest
@AutoConfigureMockRestServiceServer
@org.springframework.context.annotation.Import(com.selimhorri.app.config.TestRestTemplateConfig.class)
class OrderItemServiceImplIntegrationTest {

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanRepository() {
        orderItemRepository.deleteAll();
    }

    @Test
    @DisplayName("findAll integra Product y Order service para cada OrderItem")
    void findAllEnrichesWithRemoteServices() throws JsonProcessingException {
        List<OrderItem> items = orderItemRepository.saveAll(List.of(
            OrderItem.builder().productId(300).orderId(900).orderedQuantity(1).build(),
            OrderItem.builder().productId(301).orderId(901).orderedQuantity(2).build()
        ));

        for (OrderItem item : items) {
            ProductDto productDto = ProductDto.builder()
                .productId(item.getProductId())
                .productTitle("product-" + item.getProductId())
                .priceUnit(15.0)
                .build();
            OrderDto orderDto = OrderDto.builder()
                .orderId(item.getOrderId())
                .orderDesc("order-" + item.getOrderId())
                .orderFee(45.0)
                .orderDate(LocalDateTime.now())
                .build();

            mockServer.expect(requestTo(AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + item.getProductId()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(productDto), MediaType.APPLICATION_JSON));
            mockServer.expect(requestTo(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + item.getOrderId()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(orderDto), MediaType.APPLICATION_JSON));
        }

        List<OrderItemDto> dtos = orderItemService.findAll();

        assertThat(dtos)
            .hasSize(2)
            .allSatisfy(dto -> {
                assertThat(dto.getProductDto().getProductTitle()).startsWith("product-");
                assertThat(dto.getOrderDto().getOrderDesc()).startsWith("order-");
            });
    }
}

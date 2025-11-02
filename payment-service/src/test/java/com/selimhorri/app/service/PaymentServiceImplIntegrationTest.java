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
import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.repository.PaymentRepository;

@SpringBootTest
@AutoConfigureMockRestServiceServer
@org.springframework.context.annotation.Import(com.selimhorri.app.config.TestRestTemplateConfig.class)
class PaymentServiceImplIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanRepository() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("findById resuelve el Order Service mediante RestTemplate")
    void findByIdResolvesOrderService() throws JsonProcessingException {
        Payment payment = paymentRepository.save(Payment.builder()
            .orderId(77)
            .isPayed(true)
            .paymentStatus(PaymentStatus.COMPLETED)
            .build());

        OrderDto orderDto = OrderDto.builder()
            .orderId(77)
            .orderDesc("integration-order")
            .orderFee(35.5)
            .orderDate(LocalDateTime.now())
            .build();

        mockServer.expect(requestTo(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + payment.getOrderId()))
            .andRespond(withSuccess(objectMapper.writeValueAsString(orderDto), MediaType.APPLICATION_JSON));

        PaymentDto dto = paymentService.findById(payment.getPaymentId());

        assertThat(dto.getOrderDto().getOrderDesc()).isEqualTo("integration-order");
    assertThat(dto.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("findAll solicita los datos del Order Service por cada pago")
    void findAllEnrichesWithOrders() throws JsonProcessingException {
        List<Payment> payments = paymentRepository.saveAll(List.of(
            Payment.builder().orderId(101).isPayed(false).paymentStatus(PaymentStatus.IN_PROGRESS).build(),
            Payment.builder().orderId(102).isPayed(true).paymentStatus(PaymentStatus.COMPLETED).build()
        ));

        for (Payment payment : payments) {
            OrderDto orderDto = OrderDto.builder()
                .orderId(payment.getOrderId())
                .orderDesc("order-" + payment.getOrderId())
                .orderFee(42.0)
                .orderDate(LocalDateTime.now())
                .build();

            mockServer.expect(requestTo(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + payment.getOrderId()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(orderDto), MediaType.APPLICATION_JSON));
        }

        List<PaymentDto> dtos = paymentService.findAll();

        assertThat(dtos)
            .hasSize(2)
            .allSatisfy(dto -> assertThat(dto.getOrderDto().getOrderDesc()).startsWith("order-"));
    }
}

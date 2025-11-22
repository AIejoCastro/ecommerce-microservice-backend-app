package com.selimhorri.app.bulkhead;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.service.impl.FavouriteServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
class FavouriteBulkheadTest {

    @Autowired
    private FavouriteServiceImpl favouriteService;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    @DisplayName("Bulkhead para User-Service debe activar el fallback cuando se excede la concurrencia")
    void userServiceBulkhead_shouldTriggerFallback_whenConcurrentCallsExceedLimit() throws Exception {
        final UserDto mockUser = UserDto.builder()
                .userId(1)
                .firstName("Selim")
                .lastName("Horri")
                .build();

        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/1"),
                eq(UserDto.class))
        ).thenAnswer((Answer<UserDto>) invocation -> {
            System.out.println("HILO: " + Thread.currentThread().getName() + " -> Entrando a la llamada lenta (ocupando el bulkhead).");
            Thread.sleep(2000);
            System.out.println("HILO: " + Thread.currentThread().getName() + " -> Terminando la llamada lenta (liberando el bulkhead).");
            return mockUser;
        });


        System.out.println("Lanzando la primera llamada (debería tener éxito)");
        CompletableFuture<UserDto> firstCall = CompletableFuture.supplyAsync(
                () -> favouriteService.fetchUserById(1)
        );

        Thread.sleep(100);

        System.out.println("Lanzando la segunda llamada (debería activar el fallback)...");
        CompletableFuture<UserDto> secondCall = CompletableFuture.supplyAsync(
                () -> favouriteService.fetchUserById(1)
        );

        CompletableFuture.allOf(firstCall, secondCall).join();

        UserDto resultFromFirstCall = firstCall.get(5, TimeUnit.SECONDS);
        UserDto resultFromSecondCall = secondCall.get(5, TimeUnit.SECONDS);

        System.out.println("Resultado de la primera llamada: " + resultFromFirstCall);
        System.out.println("Resultado de la segunda llamada: " + resultFromSecondCall);

        assertThat(resultFromFirstCall).isNotNull();
        assertThat(resultFromFirstCall.getUserId()).isEqualTo(1);
        assertThat(resultFromFirstCall.getFirstName()).isEqualTo("Selim");

        assertThat(resultFromSecondCall).isNotNull();
        assertThat(resultFromSecondCall.getUserId()).isNull();
        assertThat(resultFromSecondCall.getFirstName()).isNull();
    }
}
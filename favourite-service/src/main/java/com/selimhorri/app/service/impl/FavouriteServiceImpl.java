package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.FavouriteNotFoundException;
import com.selimhorri.app.helper.FavouriteMappingHelper;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.FavouriteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class FavouriteServiceImpl implements FavouriteService {

	private final FavouriteRepository favouriteRepository;
	private final RestTemplate restTemplate;

	@Override
	@RateLimiter(name = "favouriteServiceRateLimiter", fallbackMethod = "fallbackRateLimiter")
	public List<FavouriteDto> findAll() {
		log.info("*** FavouriteDto List, service; fetch all favourites *");
		return this.favouriteRepository.findAll()
				.stream()
				.map(FavouriteMappingHelper::map)
				.map(f -> {
					f.setUserDto(fetchUserById(f.getUserId()));
					f.setProductDto(fetchProductById(f.getProductId()));
					return f;
				})
				.distinct()
				.collect(Collectors.toUnmodifiableList());
	}

	@Override
//	@RateLimiter(name = "favouriteServiceRateLimiter", fallbackMethod = "fallbackRateLimiter")
	public FavouriteDto findById(final FavouriteId favouriteId) {
		log.info("*** FavouriteDto, service; fetch favourite by id *");
		return this.favouriteRepository.findById(favouriteId)
				.map(FavouriteMappingHelper::map)
				.map(f -> {
					f.setUserDto(fetchUserById(f.getUserId()));
					f.setProductDto(fetchProductById(f.getProductId()));
					return f;
				})
				.orElseThrow(() -> new FavouriteNotFoundException(
						String.format("Favourite with id: [%s] not found!", favouriteId)));
	}

	@Override
	public FavouriteDto save(final FavouriteDto favouriteDto) {
		return FavouriteMappingHelper.map(this.favouriteRepository
				.save(FavouriteMappingHelper.map(favouriteDto)));
	}

	@Override
	public FavouriteDto update(final FavouriteDto favouriteDto) {
		return FavouriteMappingHelper.map(this.favouriteRepository
				.save(FavouriteMappingHelper.map(favouriteDto)));
	}

	@Override
	public void deleteById(final FavouriteId favouriteId) {
		this.favouriteRepository.deleteById(favouriteId);
	}

	@Bulkhead(name = "userServiceBulkhead", fallbackMethod = "fallbackUser")
	public UserDto fetchUserById(Integer userId) {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return restTemplate.getForObject(
				AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + userId,
				UserDto.class);
	}


	@Bulkhead(name = "productServiceBulkhead", fallbackMethod = "fallbackProduct")
	public ProductDto fetchProductById(Integer productId) {
		return restTemplate.getForObject(
				AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId,
				ProductDto.class);
	}

	public ProductDto fallbackProduct(Integer productId, Throwable t) {
		log.warn("Fallback triggered for product {}: {}", productId, t.toString());
		return new ProductDto();
	}

	public UserDto fallbackUser(Integer userId, Throwable t) {
		log.warn("Fallback triggered for user {}: {}", userId, t.toString());
		return new UserDto();
	}

	public List<FavouriteDto> fallbackRateLimiter(Throwable t) {
		log.warn("Favourite service does not permit further calls: {}", t.toString());
		return List.of();
	}
}
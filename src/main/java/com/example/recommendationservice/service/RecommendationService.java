package com.example.recommendationservice.service;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.model.RecommendationResponse;
import com.example.recommendationservice.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    @Autowired
    private ProductSearchRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Cacheable(value = "recommendations", key = "#userId + #page", unless = "#result == null")
    public RecommendationResponse getRecommendations(String userId, int page, int size) {
        log.debug("Fetching recommendation for UserID: {} (Page: {}, Size: {})", userId, page, size);
        String favoriteCategory =
                (String) redisTemplate.opsForValue()
                        .get("user:" + userId + ":fav_category");

        Pageable pageable = PageRequest.of(page, size);

        Page<ProductDoc> productDocPage;

        if (favoriteCategory != null) {
            log.info("Personalization: USer {} has favorite category '{}'. Fetching targeted results.", userId, favoriteCategory);
            productDocPage = productRepository.findByCategory(favoriteCategory, pageable);
        } else {
            log.info("Personalization: No favorite category found for User {}. Fetching default results.", userId);
            productDocPage = productRepository.findAll(pageable);
        }

        log.debug("Returned {} products for User {}", productDocPage.getNumberOfElements(), userId);

        return new RecommendationResponse(
                productDocPage.getContent(),
                productDocPage.getNumber(),
                productDocPage.getTotalElements(),
                productDocPage.hasNext()
        );
    }
}

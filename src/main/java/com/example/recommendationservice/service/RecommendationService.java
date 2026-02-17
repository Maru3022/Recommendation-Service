package com.example.recommendationservice.service;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.model.RecommendationResponse;
import com.example.recommendationservice.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    @Autowired
    private final ProductSearchRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public RecommendationResponse getRecommendations(String userId, int page, int size) {

        String favoriteCategory =
                (String) redisTemplate.opsForValue()
                        .get("user:" + userId + ":fav_category");

        Pageable pageable = PageRequest.of(page, size);

        Page<ProductDoc> productDocPage;

        if (favoriteCategory != null) {
            productDocPage =
                    productRepository.findByCategory(favoriteCategory, pageable);
        } else {
            productDocPage =
                    productRepository.findAll(pageable);
        }

        return new RecommendationResponse(
                productDocPage.getContent(),
                productDocPage.getNumber(),
                productDocPage.getTotalElements(),
                productDocPage.hasNext()
        );
    }
}

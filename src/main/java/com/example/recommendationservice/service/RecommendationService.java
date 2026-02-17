package com.example.recommendationservice.service;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ProductSearchRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public List<ProductDoc> getRecommendations(String userId){
        String favoriteCategory = (String) redisTemplate.opsForValue().get("user: " + userId + ":fav_category");

        if(favoriteCategory != null){
            return productRepository.findByCategory(favoriteCategory);
        }

        return StreamSupport.stream(productRepository.findAll().spliterator(),false)
                .limit(10)
                .collect(Collectors.toList());
    }

}

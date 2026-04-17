package com.example.recommendationservice.service;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.model.RecommendationResponse;
import com.example.recommendationservice.model.UserAction;
import com.example.recommendationservice.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final ProductSearchRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final RedisTemplate<String, Object> redisTemplate;

    @Cacheable(value = "recommendations", key = "#userId + ':' + #page + ':' + #size", unless = "#result == null")
    public RecommendationResponse getRecommendations(String userId, int page, int size) {
        validatePagination(page, size);
        log.debug("Fetching recommendation for UserID: {} (Page: {}, Size: {})", userId, page, size);
        String favoriteCategory = null;

        try {
            favoriteCategory = (String) redisTemplate.opsForValue()
                    .get("user:" + userId + ":fav_category");
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis is unavailable, returning non-personalized recommendations for user {}", userId);
        }

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

    public List<ProductDoc> getPopularProducts(int limit){
        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }

        NativeQuery query = NativeQuery.builder()
                .withMaxResults(0)
                .withAggregation("most_popular", Aggregation.of(a -> a
                        .terms(t -> t
                                .field("productId")
                                .size(limit)
                        )
                ))
                .build();

        SearchHits<UserAction> hits = elasticsearchOperations.search(query,UserAction.class);

        if(hits.getAggregations() == null) return new ArrayList<>();

        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) hits.getAggregations();
        List<String> popularIds = new ArrayList<>();
        var aggregate = aggregations.get("most_popular").aggregation().getAggregate();

        if(aggregate.isSterms()){
            aggregate.sterms().buckets().array().forEach(bucket -> {
                popularIds.add(bucket.key().stringValue());
            });
        }

        if(popularIds.isEmpty()) return new ArrayList<>();

        return productRepository.findAllByIdIn(popularIds);
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }

        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}

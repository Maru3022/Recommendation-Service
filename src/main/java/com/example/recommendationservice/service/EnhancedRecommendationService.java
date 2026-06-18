package com.example.recommendationservice.service;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.model.UserAction;
import com.example.recommendationservice.repository.ActionRepository;
import com.example.recommendationservice.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedRecommendationService {

    private final ProductSearchRepository productRepository;
    private final ActionRepository actionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ElasticsearchOperations elasticsearchOperations;

    public List<ProductDoc> getCollaborativeRecommendations(String userId, int limit) {
        log.debug("Getting collaborative recommendations for user: {}", userId);
        
        try {
            // Find users with similar behavior
            List<String> similarUsers = findSimilarUsers(userId, 5);
            
            if (similarUsers.isEmpty()) {
                log.debug("No similar users found for: {}", userId);
                return Collections.emptyList();
            }

            // Get products liked/viewed by similar users
            List<UserAction> similarUserActions = new ArrayList<>();
            for (String similarUser : similarUsers) {
                similarUserActions.addAll(actionRepository.findByUserId(similarUser));
            }
            
            // Extract product IDs
            Set<String> productIds = new HashSet<>();
            for (UserAction action : similarUserActions) {
                if ("like".equals(action.getActionType()) || "view".equals(action.getActionType())) {
                    productIds.add(action.getProductId());
                }
            }

            // Remove already interacted products
            Set<String> userProductIds = getUserInteractedProducts(userId);
            productIds.removeAll(userProductIds);

            if (productIds.isEmpty()) {
                return Collections.emptyList();
            }

            List<ProductDoc> products = productRepository.findAllByIdIn(new ArrayList<>(productIds));
            
            // Limit results
            return products.stream()
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting collaborative recommendations for user: {}", userId, e);
            return Collections.emptyList();
        }
    }

    public List<ProductDoc> getContentBasedRecommendations(String userId, int limit) {
        log.debug("Getting content-based recommendations for user: {}", userId);
        
        try {
            // Get user's preferred categories from Redis
            String preferenceKey = "user:" + userId + ":category_preferences";
            Map<Object, Object> preferences = redisTemplate.opsForHash().entries(preferenceKey);
            
            if (preferences.isEmpty()) {
                log.debug("No category preferences found for user: {}", userId);
                return Collections.emptyList();
            }

            // Get products from preferred categories
            List<String> preferredCategories = preferences.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare((Long) e2.getValue(), (Long) e1.getValue()))
                    .limit(3)
                    .map(entry -> (String) entry.getKey())
                    .collect(java.util.stream.Collectors.toList());

            List<ProductDoc> allProducts = new ArrayList<>();
            for (String category : preferredCategories) {
                Pageable pageable = PageRequest.of(0, limit / preferredCategories.size());
                var page = productRepository.findByCategory(category, pageable);
                if (page == null || page.getContent() == null) {
                    log.debug("No products returned for category '{}' (userId={})", category, userId);
                    continue;
                }
                allProducts.addAll(page.getContent());
            }

            // Remove already interacted products
            Set<String> userProductIds = getUserInteractedProducts(userId);
            return allProducts.stream()
                    .filter(product -> !userProductIds.contains(product.getId()))
                    .limit(limit)
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting content-based recommendations for user: {}", userId, e);
            return Collections.emptyList();
        }
    }

    public List<ProductDoc> getTrendingProducts(int limit) {
        log.debug("Getting trending products");
        
        try {
            NativeQuery query = NativeQuery.builder()
                    .withMaxResults(0)
                    .withAggregation("trending_products", Aggregation.of(a -> a
                            .terms(t -> t
                                    .field("productId")
                                    .size(limit)
                            )
                    ))
                    .build();

            SearchHits<UserAction> searchHits = elasticsearchOperations.search(query, UserAction.class);
            List<String> trendingIds = new ArrayList<>();

            if (searchHits.getAggregations() != null) {
                ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();
                var aggregate = aggregations.get("trending_products").aggregation().getAggregate();

                if (aggregate.isSterms()) {
                    aggregate.sterms().buckets().array().forEach(bucket -> {
                        trendingIds.add(bucket.key().stringValue());
                    });
                }
            }

            if (trendingIds.isEmpty()) {
                return Collections.emptyList();
            }

            return productRepository.findAllByIdIn(trendingIds);

        } catch (Exception e) {
            log.error("Error getting trending products", e);
            return Collections.emptyList();
        }
    }

    private List<String> findSimilarUsers(String userId, int limit) {
        try {
            // Get user's product interactions
            Set<String> userProducts = getUserInteractedProducts(userId);
            
            if (userProducts.isEmpty()) {
                return Collections.emptyList();
            }

            // Find other users who interacted with same products
            Map<String, Long> userInteractions = new HashMap<>();
            List<UserAction> actions = actionRepository.findByProductIdIn(new ArrayList<>(userProducts));
            for (UserAction action : actions) {
                if (!action.getUserId().equals(userId)) {
                    userInteractions.merge(action.getUserId(), 1L, Long::sum);
                }
            }

            return userInteractions.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("Error finding similar users for: {}", userId, e);
            return Collections.emptyList();
        }
    }

    private Set<String> getUserInteractedProducts(String userId) {
        try {
            List<UserAction> actions = actionRepository.findByUserId(userId);
            return actions.stream()
                    .map(UserAction::getProductId)
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            log.error("Error getting user interacted products for: {}", userId, e);
            return Collections.emptySet();
        }
    }
}

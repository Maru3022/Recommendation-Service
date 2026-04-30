package com.example.recommendationservice.replit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

/**
 * Beans that replace the Elasticsearch / Redis infrastructure in the
 * {@code replit} Spring profile. The Redis side is backed by a real
 * {@link InMemoryRedisTemplate}; the Elasticsearch side is a
 * {@link Proxy}-based stub that returns empty {@link SearchHits} (no
 * aggregations, no documents) for every call.
 *
 * <p>The {@link com.example.recommendationservice.service.RecommendationService}
 * already guards against {@code hits.getAggregations() == null} and falls
 * back to an empty list, so the popular-products endpoint stays responsive
 * even without a real cluster. The richer aggregation logic in this profile
 * is provided by
 * {@link com.example.recommendationservice.service.EnhancedRecommendationService},
 * which only depends on the in-memory action repository.</p>
 */
@Configuration
@Profile("replit")
public class ReplitMockBeansConfig {

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        return new InMemoryRedisTemplate();
    }

    @Bean
    @Primary
    public ElasticsearchOperations elasticsearchOperations() {
        return (ElasticsearchOperations) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ElasticsearchOperations.class},
                (proxy, method, args) -> {
                    Method m = ElasticsearchOperations.class.getMethod(method.getName(), method.getParameterTypes());
                    Class<?> returnType = m.getReturnType();
                    if (returnType == SearchHits.class) {
                        return new SearchHits<>() {
                            @Override
                            public float getMaxScore() { return 0; }
                            @Override
                            public long getTotalHits() { return 0; }
                            @Override
                            public TotalHitsRelation getTotalHitsRelation() { return null; }
                            @Override
                            public boolean hasAggregations() { return false; }
                            @Override
                            public Object getAggregations() { return null; }
                            @Override
                            public String getScrollId() { return null; }
                            @Override
                            public java.util.List<org.springframework.data.elasticsearch.core.SearchHit<?>> getSearchHits() {
                                return Collections.emptyList();
                            }
                            @Override
                            public float getScrollTime() { return 0; }
                            @Override
                            public boolean isEmpty() { return true; }
                        };
                    }
                    if (returnType.isPrimitive()) {
                        if (returnType == boolean.class) return false;
                        if (returnType == int.class) return 0;
                        if (returnType == long.class) return 0L;
                        if (returnType == double.class) return 0.0;
                        if (returnType == float.class) return 0.0f;
                    }
                    return null;
                }
        );
    }
}

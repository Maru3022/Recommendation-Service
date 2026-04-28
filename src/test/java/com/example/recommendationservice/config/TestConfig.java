package com.example.recommendationservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

/**
 * Test configuration that provides mock beans for external services
 * This allows tests to run without requiring actual Elasticsearch, Redis, or Kafka instances
 */
@TestConfiguration
public class TestConfig {

    @Bean
    public ElasticsearchOperations elasticsearchOperations() {
        return mock(ElasticsearchOperations.class);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        return mock(RedisTemplate.class);
    }

    @Bean
    public KafkaTemplate<?, ?> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }
}

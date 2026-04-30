package com.example.recommendationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Enables Spring Data Elasticsearch repository proxies for
 * {@link com.example.recommendationservice.repository.ProductSearchRepository}
 * and {@link com.example.recommendationservice.repository.ActionRepository}.
 *
 * <p>Disabled in the {@code replit} profile, where in-memory implementations
 * from {@link com.example.recommendationservice.replit} are used instead.</p>
 */
@Configuration
@Profile("!replit")
@EnableElasticsearchRepositories(
        basePackages = "com.example.recommendationservice.repository"
)
public class ElasticsearchConfig {
}

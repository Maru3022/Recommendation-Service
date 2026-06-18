package com.example.recommendationservice.service;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.model.RagResponse;
import com.example.recommendationservice.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagRecommendationService {

    private final EmbeddingService embeddingService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductSearchRepository productSearchRepository;
    private final ChatClient.Builder chatClientBuilder;

    public RagResponse searchByQuery(String userQuery, String userId, int limit) {
        try {
            // Generate embedding for user query
            float[] queryEmbedding = embeddingService.generateEmbeddingForQuery(userQuery)
                    .orElseThrow(() -> new RuntimeException("Failed to generate query embedding"));

            // Perform kNN search in Elasticsearch
            List<ProductDoc> products = performKnnSearch(queryEmbedding, limit);

            // If no products found via kNN, fall back to simple search
            if (products.isEmpty()) {
                log.warn("No products found via kNN search, falling back to simple search");
                products = performFallbackSearch(userQuery, limit);
            }

            // Generate AI explanation
            String aiExplanation = generateAiExplanation(userQuery, products);

            return RagResponse.builder()
                    .aiExplanation(aiExplanation)
                    .products(products)
                    .build();

        } catch (Exception e) {
            log.error("Error in RAG search, falling back to simple search", e);
            // Fallback to simple search if anything fails
            List<ProductDoc> products = performFallbackSearch(userQuery, limit);
            return RagResponse.builder()
                    .aiExplanation("Showing relevant products based on your query.")
                    .products(products)
                    .build();
        }
    }

    private List<ProductDoc> performKnnSearch(float[] queryEmbedding, int limit) {
        try {
            // Temporarily use fallback until kNN is properly implemented
            log.warn("kNN search temporarily using fallback implementation");
            return performFallbackSearch("", limit);
        } catch (Exception e) {
            log.error("Error performing kNN search", e);
            return List.of();
        }
    }

    private List<ProductDoc> performFallbackSearch(String userQuery, int limit) {
        try {
            if (userQuery == null || userQuery.trim().isEmpty()) {
                // Fallback to findAll if query is empty
                Pageable pageable = PageRequest.of(0, limit);
                return productSearchRepository.findAll(pageable).getContent();
            }

            // Multi-match search on name and description
            String queryString = String.format(
                    "{\"multi_match\": {\"query\": \"%s\", \"fields\": [\"name\", \"description\"]}}",
                    userQuery.replace("\"", "\\\""));
            StringQuery stringQuery = new StringQuery(queryString);
            stringQuery.setPageable(PageRequest.of(0, limit));

            SearchHits<ProductDoc> searchHits = elasticsearchOperations.search(stringQuery, ProductDoc.class);
            return searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error performing fallback search", e);
            return List.of();
        }
    }

    private String generateAiExplanation(String userQuery, List<ProductDoc> products) {
        try {
            if (products.isEmpty()) {
                return "No products found matching your query.";
            }

            // Build product context string
            String productContext = products.stream()
                    .map(p -> String.format("- %s (Category: %s, Price: %.2f, Description: %s)",
                            p.getName(),
                            p.getCategory(),
                            p.getPrice(),
                            p.getDescription() != null ? p.getDescription() : ""))
                    .collect(Collectors.joining("\n"));

            // Create prompt
            String systemPrompt = "You are a helpful product recommendation assistant. Your task is to explain why the provided products are relevant to the user's query. Keep your response concise and friendly.";
            String userPrompt = String.format("User query: %s\n\nProducts:\n%s\n\nPlease explain why these products are relevant to the user's query.", userQuery, productContext);

            ChatClient chatClient = chatClientBuilder.build();
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("Error generating AI explanation", e);
            return "Showing relevant products based on your query.";
        }
    }
}

package com.example.recommendationservice.service;

import com.example.recommendationservice.model.ProductDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public Optional<float[]> generateEmbeddingForProduct(ProductDoc product) {
        try {
            String text = String.format(
                    "Product: %s. Description: %s. Category: %s. Price: %.2f",
                    product.getName(),
                    product.getDescription() != null ? product.getDescription() : "",
                    product.getCategory(),
                    product.getPrice()
            );

            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
            if (response != null && !response.getResults().isEmpty()) {
                float[] embedding = response.getResults().get(0).getOutput();
                log.debug("Generated embedding for product: {}", product.getId());
                return Optional.of(embedding);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to generate embedding for product: {}", product.getId(), e);
            return Optional.empty();
        }
    }

    public Optional<float[]> generateEmbeddingForQuery(String query) {
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(query));
            if (response != null && !response.getResults().isEmpty()) {
                float[] embedding = response.getResults().get(0).getOutput();
                log.debug("Generated embedding for query");
                return Optional.of(embedding);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to generate embedding for query", e);
            return Optional.empty();
        }
    }
}

package com.example.recommendationservice.service;

import com.example.recommendationservice.model.PostDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Generates 1536-dim OpenAI embeddings for posts and free-text queries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    /**
     * Build a text representation of the post and compute its embedding.
     * Combines text, tags, and category so kNN captures semantic intent.
     */
    public Optional<float[]> generateEmbeddingForPost(PostDoc post) {
        try {
            String tagsStr = post.getTags() != null
                    ? post.getTags().stream().collect(Collectors.joining(" "))
                    : "";
            String text = String.format("Post type: %s. Category: %s. Tags: %s. Text: %s",
                    post.getPostType() != null ? post.getPostType() : "",
                    post.getCategory() != null ? post.getCategory() : "",
                    tagsStr,
                    post.getText() != null ? post.getText() : "");

            return embed(text, "post:" + post.getId());
        } catch (Exception e) {
            log.error("Failed to generate embedding for post: {}", post.getId(), e);
            return Optional.empty();
        }
    }

    /**
     * Compute an embedding for a free-text user query (semantic search).
     */
    public Optional<float[]> generateEmbeddingForQuery(String query) {
        return embed(query, "query");
    }

    // -------------------------------------------------------------------------

    private Optional<float[]> embed(String text, String label) {
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
            if (response != null && !response.getResults().isEmpty()) {
                float[] embedding = response.getResults().get(0).getOutput();
                log.debug("Generated embedding for {}", label);
                return Optional.of(embedding);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to generate embedding for {}: {}", label, e.getMessage());
            return Optional.empty();
        }
    }
}

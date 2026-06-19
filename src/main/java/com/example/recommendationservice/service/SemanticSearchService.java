package com.example.recommendationservice.service;

import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.model.SearchResponse;
import com.example.recommendationservice.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Semantic (RAG) search over the posts index.
 *
 * <ol>
 *   <li>Embed the user's free-text query via OpenAI</li>
 *   <li>Perform kNN search on {@code posts.embedding} in Elasticsearch</li>
 *   <li>Fall back to multi_match on text/tags/category if kNN returns nothing</li>
 *   <li>Ask GPT-4o-mini to explain why the results are relevant</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchService {

    private final EmbeddingService        embeddingService;
    private final ElasticsearchOperations elasticsearchOperations;
    private final PostSearchRepository    postSearchRepository;
    private final ChatClient.Builder      chatClientBuilder;

    // -------------------------------------------------------------------------

    public SearchResponse searchByQuery(String userQuery, String userId, int limit) {
        try {
            float[] queryEmbedding = embeddingService.generateEmbeddingForQuery(userQuery)
                    .orElseThrow(() -> new RuntimeException("Failed to generate query embedding"));

            List<PostDoc> posts = performKnnSearch(queryEmbedding, limit);

            if (posts.isEmpty()) {
                log.warn("kNN returned no results for query '{}', falling back to multi_match", userQuery);
                posts = performFallbackSearch(userQuery, limit);
            }

            String explanation = generateAiExplanation(userQuery, posts);
            return SearchResponse.builder()
                    .aiExplanation(explanation)
                    .posts(posts)
                    .build();

        } catch (Exception e) {
            log.error("Error in semantic search, falling back", e);
            List<PostDoc> posts = performFallbackSearch(userQuery, limit);
            return SearchResponse.builder()
                    .aiExplanation("Showing posts relevant to your query.")
                    .posts(posts)
                    .build();
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<PostDoc> performKnnSearch(float[] queryEmbedding, int limit) {
        try {
            int numCandidates = Math.max(limit * 10, 50);

            // Build the float[] into a JSON array
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < queryEmbedding.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(queryEmbedding[i]);
            }
            sb.append("]");

            String queryStr = String.format(
                    "{\"knn\":{\"field\":\"embedding\",\"query_vector\":%s,\"k\":%d,\"num_candidates\":%d},\"size\":%d}",
                    sb, limit, numCandidates, limit);

            StringQuery esQuery = new StringQuery(queryStr);
            SearchHits<PostDoc> hits = elasticsearchOperations.search(esQuery, PostDoc.class);

            List<PostDoc> posts = hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            log.info("kNN search returned {} posts", posts.size());
            return posts;
        } catch (Exception e) {
            log.error("kNN search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<PostDoc> performFallbackSearch(String userQuery, int limit) {
        try {
            if (userQuery == null || userQuery.isBlank()) {
                return postSearchRepository.findAll(PageRequest.of(0, limit)).getContent();
            }
            String safe = userQuery.replace("\"", "\\\"");
            String queryStr = String.format(
                    "{\"multi_match\":{\"query\":\"%s\",\"fields\":[\"text\",\"tags\",\"category\",\"postType\"]}}",
                    safe);

            StringQuery esQuery = new StringQuery(queryStr);
            esQuery.setPageable(PageRequest.of(0, limit));
            SearchHits<PostDoc> hits = elasticsearchOperations.search(esQuery, PostDoc.class);
            return hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Fallback search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String generateAiExplanation(String userQuery, List<PostDoc> posts) {
        try {
            if (posts.isEmpty()) return "No posts found matching your query.";

            String postContext = posts.stream()
                    .map(p -> String.format("- [%s] by %s | tags: %s | text: %s",
                            p.getPostType(),
                            p.getAuthorDisplayName() != null ? p.getAuthorDisplayName() : "unknown",
                            p.getTags() != null ? String.join(", ", p.getTags()) : "",
                            p.getText() != null ? p.getText().substring(0, Math.min(p.getText().length(), 120)) : ""))
                    .collect(Collectors.joining("\n"));

            String systemPrompt = "You are a helpful fitness social feed assistant. Explain concisely why the provided posts are relevant to the user's query. Be friendly and motivational.";
            String userPrompt   = String.format("User query: %s\n\nPosts:\n%s\n\nWhy are these posts relevant?",
                    userQuery, postContext);

            return chatClientBuilder.build()
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Failed to generate AI explanation: {}", e.getMessage());
            return "Showing posts relevant to your query.";
        }
    }
}

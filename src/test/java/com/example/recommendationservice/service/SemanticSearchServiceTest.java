package com.example.recommendationservice.service;

import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.repository.PostSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {

    @Mock private PostSearchRepository postSearchRepository;
    @Mock private EmbeddingService embeddingService;
    @Mock private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private SemanticSearchService semanticSearchService;

    private PostDoc testPost;

    @BeforeEach
    void setUp() {
        testPost = new PostDoc();
        testPost.setId("post1");
        testPost.setAuthorId("author1");
        testPost.setText("Test post about fitness");
        testPost.setCreatedAt(Instant.now());
    }

    @Test
    void semanticSearch_returnsSimilarPosts() {
        when(embeddingService.generateEmbedding("fitness")).thenReturn(new float[1536]);
        
        SearchHit<PostDoc> searchHit = new SearchHit<>(null, null, 1.0f, null, testPost);
        SearchHits<PostDoc> searchHits = new SearchHits<>(List.of(searchHit), null, null);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(PostDoc.class))).thenReturn(searchHits);

        List<PostDoc> result = semanticSearchService.semanticSearch("fitness", 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("post1");
    }

    @Test
    void semanticSearch_emptyQueryReturnsEmptyList() {
        List<PostDoc> result = semanticSearchService.semanticSearch("", 10);

        assertThat(result).isEmpty();
        verify(embeddingService, never()).generateEmbedding(anyString());
    }

    @Test
    void semanticSearch_handlesEmbeddingFailure() {
        when(embeddingService.generateEmbedding("fitness")).thenThrow(new RuntimeException("AI service error"));

        List<PostDoc> result = semanticSearchService.semanticSearch("fitness", 10);

        assertThat(result).isEmpty();
    }
}

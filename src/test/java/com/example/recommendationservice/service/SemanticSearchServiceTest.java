package com.example.recommendationservice.service;

import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.model.SearchResponse;
import com.example.recommendationservice.repository.PostSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {

    @Mock private EmbeddingService embeddingService;
    @Mock private ElasticsearchOperations elasticsearchOperations;
    @Mock private PostSearchRepository postSearchRepository;
    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec callResponseSpec;

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
    void searchByQuery_returnsSimilarPosts() {
        when(embeddingService.generateEmbeddingForQuery("fitness")).thenReturn(Optional.of(new float[1536]));

        @SuppressWarnings("unchecked")
        SearchHits<PostDoc> searchHits = mock(SearchHits.class);
        SearchHit<PostDoc> searchHit = mock(SearchHit.class);
        when(searchHit.getContent()).thenReturn(testPost);
        when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
        when(elasticsearchOperations.search(any(StringQuery.class), eq(PostDoc.class))).thenReturn(searchHits);

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("These posts match your fitness query.");

        SearchResponse result = semanticSearchService.searchByQuery("fitness", "user1", 10);

        assertThat(result.getPosts()).hasSize(1);
        assertThat(result.getPosts().get(0).getId()).isEqualTo("post1");
        assertThat(result.getAiExplanation()).contains("fitness");
    }

    @Test
    void searchByQuery_fallsBackWhenEmbeddingFails() {
        when(embeddingService.generateEmbeddingForQuery("fitness")).thenReturn(Optional.empty());

        @SuppressWarnings("unchecked")
        SearchHits<PostDoc> searchHits = mock(SearchHits.class);
        SearchHit<PostDoc> searchHit = mock(SearchHit.class);
        when(searchHit.getContent()).thenReturn(testPost);
        when(searchHits.getSearchHits()).thenReturn(List.of(searchHit));
        when(elasticsearchOperations.search(any(StringQuery.class), eq(PostDoc.class))).thenReturn(searchHits);

        SearchResponse result = semanticSearchService.searchByQuery("fitness", "user1", 10);

        assertThat(result.getPosts()).hasSize(1);
        assertThat(result.getAiExplanation()).isNotBlank();
    }

    @Test
    void searchByQuery_blankQueryFallsBackToRepository() {
        when(postSearchRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(testPost)));

        SearchResponse result = semanticSearchService.searchByQuery("", "user1", 10);

        assertThat(result.getPosts()).hasSize(1);
    }
}

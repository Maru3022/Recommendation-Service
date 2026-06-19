package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.model.SearchRequest;
import com.example.recommendationservice.model.SearchResponse;
import com.example.recommendationservice.service.SemanticSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticSearchControllerTest {

    @Mock private SemanticSearchService semanticSearchService;

    @InjectMocks
    private SemanticSearchController semanticSearchController;

    @Test
    void search_returnsSearchResults() {
        SearchRequest request = new SearchRequest("fitness tips", "user1");
        SearchResponse expected = SearchResponse.builder()
                .aiExplanation("These posts match your fitness query.")
                .posts(List.of(new PostDoc()))
                .build();

        when(semanticSearchService.searchByQuery("fitness tips", "user1", 10)).thenReturn(expected);

        ResponseEntity<SearchResponse> result = semanticSearchController.search(request);

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getPosts()).hasSize(1);
        assertThat(result.getBody().getAiExplanation()).contains("fitness");
    }
}

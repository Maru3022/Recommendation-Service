package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.SearchRequest;
import com.example.recommendationservice.model.SearchResponse;
import com.example.recommendationservice.service.SemanticSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticSearchControllerTest {

    @Mock private SemanticSearchService semanticSearchService;

    @InjectMocks
    private SemanticSearchController semanticSearchController;

    @Test
    void search_returnsSearchResults() {
        SearchRequest request = new SearchRequest();
        request.setQuery("fitness");
        request.setLimit(10);

        when(semanticSearchService.semanticSearch("fitness", 10)).thenReturn(List.of());

        ResponseEntity<SearchResponse> result = semanticSearchController.search(request);

        assertThat(result.getBody()).isNotNull();
    }
}

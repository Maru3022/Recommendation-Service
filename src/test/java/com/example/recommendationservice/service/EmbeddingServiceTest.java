package com.example.recommendationservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock private OpenAiEmbeddingModel embeddingModel;

    @InjectMocks
    private EmbeddingService embeddingService;

    @Test
    void generateEmbedding_returnsEmbeddingVector() {
        org.springframework.ai.embedding.Embedding embedding = new org.springframework.ai.embedding.Embedding(new float[1536], 0);
        EmbeddingResponse response = new EmbeddingResponse(List.of(embedding));
        
        when(embeddingModel.call(anyString(), any(OpenAiEmbeddingOptions.class))).thenReturn(response);

        float[] result = embeddingService.generateEmbedding("test text");

        assertThat(result).hasSize(1536);
    }

    @Test
    void generateEmbedding_handlesEmptyText() {
        org.springframework.ai.embedding.Embedding embedding = new org.springframework.ai.embedding.Embedding(new float[1536], 0);
        EmbeddingResponse response = new EmbeddingResponse(List.of(embedding));
        
        when(embeddingModel.call(anyString(), any(OpenAiEmbeddingOptions.class))).thenReturn(response);

        float[] result = embeddingService.generateEmbedding("");

        assertThat(result).hasSize(1536);
    }
}

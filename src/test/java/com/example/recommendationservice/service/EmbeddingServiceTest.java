package com.example.recommendationservice.service;

import com.example.recommendationservice.model.PostDoc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock private EmbeddingModel embeddingModel;

    @InjectMocks
    private EmbeddingService embeddingService;

    @Test
    void generateEmbeddingForQuery_returnsEmbeddingVector() {
        org.springframework.ai.embedding.Embedding embedding =
                new org.springframework.ai.embedding.Embedding(new float[1536], 0);
        when(embeddingModel.embedForResponse(any())).thenReturn(new EmbeddingResponse(List.of(embedding)));

        Optional<float[]> result = embeddingService.generateEmbeddingForQuery("test text");

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1536);
    }

    @Test
    void generateEmbeddingForPost_returnsEmbeddingVector() {
        PostDoc post = PostDoc.builder()
                .id("post1")
                .text("Workout complete")
                .postType("WORKOUT_COMPLETED")
                .category("FITNESS")
                .tags(List.of("cardio"))
                .build();

        org.springframework.ai.embedding.Embedding embedding =
                new org.springframework.ai.embedding.Embedding(new float[1536], 0);
        when(embeddingModel.embedForResponse(any())).thenReturn(new EmbeddingResponse(List.of(embedding)));

        Optional<float[]> result = embeddingService.generateEmbeddingForPost(post);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1536);
    }
}

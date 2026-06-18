package com.example.recommendationservice;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.repository.ProductSearchRepository;
import com.example.recommendationservice.service.EmbeddingService;
import com.example.recommendationservice.service.ProductSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class ProductSyncServiceTest {

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private ProductSyncService productSyncService;

    @org.junit.jupiter.api.BeforeEach
    void setUp(){
        float[] sampleEmbedding = new float[1536];
        lenient().when(embeddingService.generateEmbeddingForProduct(any())).thenReturn(Optional.of(sampleEmbedding));
    }

    @Test
    void saveProduct_Success_ShouldSaveToRepo(){
        float[] sampleEmbedding = new float[1536];
        ProductDoc productDoc = new ProductDoc("1","Test","Test description","Cat",10.0,"url", sampleEmbedding);
        productSyncService.saveProduct(productDoc);
        verify(productSearchRepository).save(productDoc);
    }

    @Test
    void saveProduct_Exception_ShouldHandleGracefully(){
        float[] sampleEmbedding = new float[1536];
        ProductDoc productDoc = new ProductDoc("1","Test","Test description","Cat",10.0,"url", sampleEmbedding);

        when(productSearchRepository.save(any())).thenThrow(new RuntimeException("ES Down"));

        assertThrows(RuntimeException.class, () -> productSyncService.saveProduct(productDoc));
        verify(productSearchRepository).save(productDoc);
    }

}

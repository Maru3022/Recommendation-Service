package com.example.recommendationservice;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.repository.ProductSearchRepository;
import com.example.recommendationservice.service.ProductSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductSyncServiceTest {

    @Mock
    private ProductSearchRepository productSearchRepository;

    @InjectMocks
    private ProductSyncService productSyncService;

    @Test
    void saveProduct_Success_ShouldSaveToRepo(){
        ProductDoc productDoc = new ProductDoc("1","Test","Cat",10.0,"url");
        productSyncService.saveProduct(productDoc);
        verify(productSearchRepository).save(productDoc);
    }

    @Test
    void saveProduct_Exception_ShouldHandleGracefully(){
        ProductDoc productDoc = new ProductDoc("1","Test","Cat",10.0,"url");

        when(productSearchRepository.save(any())).thenThrow(new RuntimeException("ES Down"));

        productSyncService.saveProduct(productDoc);
        verify(productSearchRepository).save(productDoc);
    }

}
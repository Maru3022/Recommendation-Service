package com.example.recommendationservice;

import com.example.recommendationservice.consumer.ProductSyncConsumer;
import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.service.ProductSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ProductSyncConsumerTest {

    @Mock
    private ProductSyncService productSyncService;

    @InjectMocks
    private ProductSyncConsumer productSyncConsumer;

    @Test
    void consumeProductUpdate_ShouldCallService(){
        ProductDoc productDoc = new ProductDoc("p1","Laptop","Tech",1500.0, "img_url");

        productSyncConsumer.consumeProductUpdate(productDoc);

        verify(productSyncService, times(1)).saveProduct(productDoc);
    }
}

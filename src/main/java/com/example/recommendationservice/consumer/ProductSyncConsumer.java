package com.example.recommendationservice.consumer;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.service.ProductSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Profile("!replit")
@RequiredArgsConstructor
public class ProductSyncConsumer {

    private final ProductSyncService productSyncService;

    @KafkaListener(topics = "product-updates", groupId = "rec-group")
    public void consumeProductUpdate(ProductDoc product){
        productSyncService.saveProduct(product);
    }

}
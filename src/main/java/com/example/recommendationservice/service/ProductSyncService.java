package com.example.recommendationservice.service;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSyncService {

    private final ProductSearchRepository productSearchRepository;
    private final EmbeddingService embeddingService;

    public void saveProduct(ProductDoc productDoc) {
        try {
            embeddingService.generateEmbeddingForProduct(productDoc)
                    .ifPresent(productDoc::setEmbedding);
            productSearchRepository.save(productDoc);
            log.debug("ElasticSearch: Product {} successfully indexed", productDoc.getId());
        } catch (Exception e) {
            log.error("ElasticSearch: Failed to index product {}. Error: {}", productDoc.getId(), e.getMessage());
            throw e;
        }
    }

}

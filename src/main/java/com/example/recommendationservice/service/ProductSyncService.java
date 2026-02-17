package com.example.recommendationservice.service;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductSyncService {

    private final ProductSearchRepository productSearchRepository;
    public void saveProduct(ProductDoc productDoc){
        productSearchRepository.save(productDoc);
    }

}

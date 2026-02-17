package com.example.recommendationservice.repository;

import com.example.recommendationservice.model.ProductDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSearchRepository
        extends ElasticsearchRepository<ProductDoc, String> {

    Page<ProductDoc> findByCategory(String category, Pageable pageable);
}

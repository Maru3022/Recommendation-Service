package com.example.recommendationservice.repository;

import com.example.recommendationservice.model.ProductDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDoc, String> {
    List<ProductDoc> findByCategory(String category);
}
package com.example.recommendationservice.repository;

import com.example.recommendationservice.model.ProductDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionRepository
        extends ElasticsearchRepository<ProductDoc, String> {

    List<ProductDoc> findByCategory(String category);
}

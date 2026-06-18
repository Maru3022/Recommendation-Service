package com.example.recommendationservice.repository;

import com.example.recommendationservice.model.UserAction;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionRepository extends ElasticsearchRepository<UserAction, String> {
    
    List<UserAction> findByUserId(String userId);
    List<UserAction> findByProductId(String productId);
    List<UserAction> findByProductIdIn(List<String> productIds);
}
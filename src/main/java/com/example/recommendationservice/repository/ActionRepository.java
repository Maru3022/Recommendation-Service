package com.example.recommendationservice.repository;

import com.example.recommendationservice.model.UserAction;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionRepository extends ElasticsearchRepository<UserAction, String> {
}
package com.example.recommendationservice.repository;

import com.example.recommendationservice.model.PostDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostSearchRepository extends ElasticsearchRepository<PostDoc, String> {

    Page<PostDoc> findByCategory(String category, Pageable pageable);

    List<PostDoc> findAllByIdIn(List<String> ids);
}

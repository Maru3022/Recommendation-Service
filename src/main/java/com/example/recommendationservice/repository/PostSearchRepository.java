package com.example.recommendationservice.repository;

import com.example.recommendationservice.model.PostDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostSearchRepository extends ElasticsearchRepository<PostDoc, String> {

    List<PostDoc> findByAuthorIdIn(List<String> authorIds, Pageable pageable);

    Page<PostDoc> findByCategoryOrderByLikesCountDesc(String category, Pageable pageable);

    List<PostDoc> findByTagsIn(List<String> tags, Pageable pageable);

    List<PostDoc> findByIdIn(List<String> ids);
}
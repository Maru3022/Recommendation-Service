package com.example.recommendationservice.repository;

import com.example.recommendationservice.model.PostAction;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostActionRepository extends ElasticsearchRepository<PostAction, String> {

    List<PostAction> findByUserId(String userId);

    List<PostAction> findByPostId(String postId);

    List<PostAction> findByPostIdIn(List<String> postIds);
}

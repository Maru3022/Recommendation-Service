package com.example.recommendationservice.repository;

import com.example.recommendationservice.model.UserProfileDoc;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserProfileSearchRepository extends ElasticsearchRepository<UserProfileDoc, String> {

    @Query("""
        {
          "knn": {
            "field": "interestEmbedding",
            "query_vector": ?0,
            "k": ?1,
            "num_candidates": ?2
          }
        }
        """)
    List<UserProfileDoc> findSimilarByKnn(float[] embedding, int k, int numCandidates);
}

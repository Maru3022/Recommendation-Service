package com.example.recommendationservice.repository;

import com.example.recommendationservice.model.PostAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostActionRepository extends JpaRepository<PostAction, Long> {

    Optional<PostAction> findByUserIdAndPostIdAndActionType(String userId, String postId, PostAction.ActionType actionType);

    List<PostAction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    @Query("SELECT pa.postId, COUNT(pa) as cnt FROM PostAction pa WHERE pa.createdAt >= :since AND pa.actionType != 'SKIP' GROUP BY pa.postId ORDER BY cnt DESC")
    List<Object[]> findTrendingPostIds(@Param("since") Instant since, Pageable pageable);

    long countByPostIdAndActionType(String postId, PostAction.ActionType actionType);
}
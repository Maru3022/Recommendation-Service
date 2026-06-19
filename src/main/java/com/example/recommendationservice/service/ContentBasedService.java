package com.example.recommendationservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.recommendationservice.config.FeedProperties;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.model.UserProfile;
import com.example.recommendationservice.repository.PostSearchRepository;
import com.example.recommendationservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentBasedService {

    private final UserProfileService userProfileService;
    private final UserProfileRepository userProfileRepository;
    private final PostSearchRepository postSearchRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final FeedProperties feedProperties;

    public List<PostDoc> getContentBasedPosts(String userId, int limit, Set<String> excludeIds) {
        float[] embedding = userProfileService.getInterestEmbedding(userId).orElse(null);
        if (embedding == null) {
            return getFallbackPosts(userId, limit, excludeIds);
        }

        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        Set<String> viewed = profile != null ? new java.util.HashSet<>(profile.getViewedPostIds()) : Collections.emptySet();

        try {
            return performKnnSearch(embedding, limit, excludeIds, viewed);
        } catch (Exception e) {
            log.warn("kNN search failed, falling back: {}", e.getMessage());
            return getFallbackPosts(userId, limit, excludeIds);
        }
    }

    private List<PostDoc> performKnnSearch(float[] embedding, int limit, Set<String> excludeIds, Set<String> viewedIds) {
        int knnCandidates = Math.max(
                feedProperties.getKnnMinCandidates(),
                limit * feedProperties.getKnnCandidatesMultiplier()
        );

        try {
            float[] queryVector = embedding;
            SearchResponse<PostDoc> response = elasticsearchClient.search(s -> s
                            .index("posts")
                            .knn(k -> k
                                    .field("embedding")
                                    .queryVector(floatArrayToList(queryVector))
                                    .k(knnCandidates)
                                    .numCandidates(knnCandidates * 2)
                            )
                            .size(knnCandidates),
                    PostDoc.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(p -> p != null)
                    .filter(p -> !excludeIds.contains(p.getId()))
                    .filter(p -> !viewedIds.contains(p.getId()))
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("kNN search error: {}", e.getMessage());
            throw new RuntimeException("kNN search failed", e);
        }
    }

    private List<Float> floatArrayToList(float[] arr) {
        List<Float> list = new java.util.ArrayList<>(arr.length);
        for (float f : arr) list.add(f);
        return list;
    }

    private List<PostDoc> getFallbackPosts(String userId, int limit, Set<String> excludeIds) {
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        if (profile == null || profile.getPreferredCategories().isEmpty()) {
            return Collections.emptyList();
        }
        String category = profile.getPreferredCategories().get(0);
        return postSearchRepository.findByCategoryOrderByLikesCountDesc(
                        category,
                        org.springframework.data.domain.PageRequest.of(0, limit * 2)
                ).stream()
                .filter(p -> !excludeIds.contains(p.getId()))
                .limit(limit)
                .toList();
    }
}
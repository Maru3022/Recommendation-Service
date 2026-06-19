package com.example.recommendationservice.service;

import com.example.recommendationservice.config.FeedProperties;
import com.example.recommendationservice.dto.PostSummaryDto;
import com.example.recommendationservice.model.PostDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedScoringService {

    private final SocialSignalService socialSignalService;
    private final CollaborativeFilteringService collaborativeFilteringService;
    private final ContentBasedService contentBasedService;
    private final TrendingService trendingService;
    private final ScoringService scoringService;
    private final FeedProperties feedProperties;

    public List<PostSummaryDto> buildFeed(String userId, int candidateCount, Set<String> excludeIds) {
        int perSource = candidateCount / 4;
        Set<String> allExclude = new HashSet<>(excludeIds);

        List<PostDoc> socialPosts = socialSignalService.getPostsFromFollowing(userId, perSource, allExclude);
        allExclude.addAll(socialPosts.stream().map(PostDoc::getId).toList());

        List<PostDoc> collaborativePosts = collaborativeFilteringService.getCollaborativePosts(userId, perSource, allExclude);
        allExclude.addAll(collaborativePosts.stream().map(PostDoc::getId).toList());

        List<PostDoc> contentPosts = contentBasedService.getContentBasedPosts(userId, perSource, allExclude);
        allExclude.addAll(contentPosts.stream().map(PostDoc::getId).toList());

        List<PostDoc> trendingPosts = trendingService.getTrendingPosts(perSource * 2).stream()
                .filter(p -> !allExclude.contains(p.getId()))
                .limit(perSource)
                .toList();

        Set<String> trendingIds = trendingPosts.stream().map(PostDoc::getId).collect(Collectors.toSet());
        Map<String, Double> trendingScores = trendingPosts.stream()
                .collect(Collectors.toMap(PostDoc::getId, 
                    post -> scoringService.calculateEngagementScore(
                        post.getLikesCount(), post.getCommentsCount(), 
                        post.getSavesCount(), post.getSharesCount())));

        Map<String, PostDoc> allCandidates = new LinkedHashMap<>();
        socialPosts.forEach(p -> allCandidates.put(p.getId(), p));
        collaborativePosts.forEach(p -> allCandidates.put(p.getId(), p));
        contentPosts.forEach(p -> allCandidates.put(p.getId(), p));
        trendingPosts.forEach(p -> allCandidates.put(p.getId(), p));

        Set<String> socialIds = socialPosts.stream().map(PostDoc::getId).collect(Collectors.toSet());
        Set<String> collaborativeIds = collaborativePosts.stream().map(PostDoc::getId).collect(Collectors.toSet());
        Set<String> contentIds = contentPosts.stream().map(PostDoc::getId).collect(Collectors.toSet());

        return allCandidates.values().stream()
                .map(post -> {
                    double social = socialIds.contains(post.getId()) ? 1.0 : 0.0;
                    double collaborative = collaborativeIds.contains(post.getId()) ? 1.0 : 0.0;
                    double content = contentIds.contains(post.getId()) ? 1.0 : 0.0;
                    double trending = trendingScores.getOrDefault(post.getId(), 0.0);

                    double score = scoringService.calculateFeedScore(
                            social, collaborative, content, trending, post.getCreatedAt());

                    return PostSummaryDto.builder()
                            .postId(post.getId())
                            .authorId(post.getAuthorId())
                            .text(post.getText())
                            .postType(post.getPostType())
                            .category(post.getCategory())
                            .tags(post.getTags())
                            .likesCount(post.getLikesCount())
                            .commentsCount(post.getCommentsCount())
                            .score(score)
                            .createdAt(post.getCreatedAt())
                            .build();
                })
                .sorted(Comparator.comparingDouble(PostSummaryDto::getScore).reversed())
                .collect(Collectors.toList());
    }
}
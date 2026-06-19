package com.example.recommendationservice.service;

import com.example.recommendationservice.model.FeedResponse;
import com.example.recommendationservice.model.PostAction;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.repository.PostActionRepository;
import com.example.recommendationservice.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid feed ranking service.
 *
 * <p>Final score = w_social   * socialScore
 *               + w_collab   * collaborativeScore
 *               + w_content  * contentScore
 *               + w_trending * trendingScore
 *               + w_freshness * freshnessScore
 * </p>
 *
 * <p>Weights are externalised in {@code application.properties} under
 * {@code recommendation.feed.weights.*} for future A/B testing.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedRankingService {

    private static final int TRENDING_WINDOW_HOURS = 72;
    private static final int MAX_AUTHOR_PER_PAGE    = 2;   // diversity guard

    private final PostSearchRepository    postSearchRepository;
    private final PostActionRepository    postActionRepository;
    private final SocialGraphService      socialGraphService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ScoringService         scoringService;

    // =========================================================================
    // Main personalized feed
    // =========================================================================

    /**
     * Returns a hybrid-ranked, paginated feed for the given user.
     */
    public FeedResponse getPersonalizedFeed(String userId, int page, int size) {
        log.debug("Building personalized feed for user={} page={} size={}", userId, page, size);

        // 1. Gather candidates
        List<PostDoc> candidates = gatherCandidates(userId, page, size);

        // 2. Score each candidate
        Set<String> following     = socialGraphService.getFollowing(userId);
        Set<String> muted         = socialGraphService.getMuted(userId);
        Set<String> trendingIds   = getTrendingPostIds(TRENDING_WINDOW_HOURS, candidates.size() * 2);
        Set<String> seenPostIds   = getUserInteractedPostIds(userId);
        Map<String, Double> contentScores = computeContentScores(userId, candidates);

        List<FeedResponse.RankedPost> ranked = candidates.stream()
                .filter(p -> !muted.contains(p.getAuthorId()))          // negative signal: muted authors
                .filter(p -> p.getVisibility() == null || "PUBLIC".equalsIgnoreCase(p.getVisibility()))
                .map(post -> {
                    double score = hybridScore(post, userId, following, trendingIds, seenPostIds, contentScores);
                    return FeedResponse.RankedPost.builder().post(post).score(score).build();
                })
                .sorted(Comparator.comparingDouble(FeedResponse.RankedPost::getScore).reversed())
                .collect(Collectors.toList());

        // 3. Diversity guard: max MAX_AUTHOR_PER_PAGE posts per author per page
        List<FeedResponse.RankedPost> diversified = applyDiversityGuard(ranked, size);

        long total = postSearchRepository.count();
        boolean hasNext = (long) (page + 1) * size < total;

        return FeedResponse.builder()
                .posts(diversified)
                .currentPage(page)
                .totalElements(total)
                .hasNext(hasNext)
                .build();
    }

    // =========================================================================
    // Chronological following-only feed
    // =========================================================================

    public FeedResponse getFollowingFeed(String userId, int page, int size) {
        Set<String> following = socialGraphService.getFollowing(userId);
        if (following.isEmpty()) {
            return emptyFeed(page);
        }

        Pageable pageable = PageRequest.of(page, size);
        // Use ES terms query to fetch posts from followed users sorted by createdAt
        String termsJson = following.stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",", "[", "]"));

        String queryStr = "{\"bool\":{\"filter\":[" +
                "{\"terms\":{\"authorId\":" + termsJson + "}}," +
                "{\"term\":{\"visibility\":\"PUBLIC\"}}" +
                "],\"must_not\":[{\"terms\":{\"authorId\":" +
                toJsonArray(socialGraphService.getMuted(userId)) + "}}]}}";

        StringQuery esQuery = new StringQuery(queryStr);
        esQuery.setPageable(pageable);

        SearchHits<PostDoc> hits = elasticsearchOperations.search(esQuery, PostDoc.class);

        List<PostDoc> posts = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .sorted(Comparator.comparing(PostDoc::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        long total = hits.getTotalHits();
        return FeedResponse.builder()
                .posts(posts.stream()
                        .map(p -> FeedResponse.RankedPost.builder().post(p).score(0).build())
                        .collect(Collectors.toList()))
                .currentPage(page)
                .totalElements(total)
                .hasNext((long)(page + 1) * size < total)
                .build();
    }

    // =========================================================================
    // Trending feed (global)
    // =========================================================================

    public List<PostDoc> getTrendingPosts(int limit) {
        log.debug("Getting trending posts (last {} h)", TRENDING_WINDOW_HOURS);
        try {
            String windowStart = Instant.now()
                    .minus(TRENDING_WINDOW_HOURS, ChronoUnit.HOURS)
                    .toString();

            NativeQuery query = NativeQuery.builder()
                    .withMaxResults(0)
                    .withQuery(Query.of(q -> q.range(r ->
                            r.field("createdAt").gte(co.elastic.clients.json.JsonData.of(windowStart)))))
                    .withAggregation("trending_posts", Aggregation.of(a -> a
                            .terms(t -> t.field("postId").size(limit))))
                    .build();

            SearchHits<PostAction> hits = elasticsearchOperations.search(query, PostAction.class);
            List<String> trendingIds = extractTermsBucketKeys(hits, "trending_posts");

            if (trendingIds.isEmpty()) {
                // fall back to most-recent posts
                return fetchRecentPosts(limit);
            }

            return postSearchRepository.findByIdIn(trendingIds);
        } catch (Exception e) {
            log.error("Error getting trending posts, falling back to recent", e);
            return fetchRecentPosts(limit);
        }
    }

    // =========================================================================
    // Raw collaborative signal
    // =========================================================================

    public List<PostDoc> getCollaborativeRecommendations(String userId, int limit) {
        try {
            List<String> similarUsers = findSimilarUsers(userId, 5);
            if (similarUsers.isEmpty()) return Collections.emptyList();

            Set<String> seenByUser = getUserInteractedPostIds(userId);
            Set<String> candidateIds = new HashSet<>();

            for (String similar : similarUsers) {
                postActionRepository.findByUserId(similar).stream()
                        .filter(a -> a.getActionType() == PostAction.ActionType.LIKE
                                || a.getActionType() == PostAction.ActionType.SAVE)
                        .map(PostAction::getPostId)
                        .filter(id -> !seenByUser.contains(id))
                        .forEach(candidateIds::add);
            }

            if (candidateIds.isEmpty()) return Collections.emptyList();

            return postSearchRepository.findByIdIn(new ArrayList<>(candidateIds))
                    .stream().limit(limit).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error in collaborative recommendations for user {}", userId, e);
            return Collections.emptyList();
        }
    }

    // =========================================================================
    // Raw content-based signal
    // =========================================================================

    public List<PostDoc> getContentBasedRecommendations(String userId, int limit) {
        try {
            String prefKey = "user:" + userId + ":category_preferences";
            Map<Object, Object> preferences = redisTemplate.opsForHash().entries(prefKey);
            if (preferences.isEmpty()) return Collections.emptyList();

            List<String> topCategories = preferences.entrySet().stream()
                    .filter(e -> !e.getKey().toString().startsWith("tag:"))
                    .sorted((e1, e2) -> Long.compare(toLong(e2.getValue()), toLong(e1.getValue())))
                    .limit(3)
                    .map(e -> e.getKey().toString())
                    .collect(Collectors.toList());

            Set<String> seen = getUserInteractedPostIds(userId);
            List<PostDoc> result = new ArrayList<>();

            for (String category : topCategories) {
                int perCat = Math.max(1, limit / topCategories.size());
                Pageable p = PageRequest.of(0, perCat);
                postSearchRepository.findByCategory(category, p)
                        .stream()
                        .filter(post -> !seen.contains(post.getId()))
                        .forEach(result::add);
            }

            return result.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error in content-based recommendations for user {}", userId, e);
            return Collections.emptyList();
        }
    }

    // =========================================================================
    // Internal scoring helpers
    // =========================================================================

    private List<PostDoc> gatherCandidates(String userId, int page, int size) {
        // Pull a larger candidate pool (3× page size) to have room after scoring/filtering
        int candidateSize = Math.min(size * 3, 300);
        Pageable pageable = PageRequest.of(page, candidateSize);

        try {
            // Prefer PUBLIC posts; sort handled by scorer afterwards
            String q = "{\"term\":{\"visibility\":\"PUBLIC\"}}";
            StringQuery esQuery = new StringQuery(q);
            esQuery.setPageable(pageable);
            SearchHits<PostDoc> hits = elasticsearchOperations.search(esQuery, PostDoc.class);
            return hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error gathering candidates for user {}", userId, e);
            return Collections.emptyList();
        }
    }

    private double hybridScore(PostDoc post, String userId,
                                Set<String> following, Set<String> trendingIds,
                                Set<String> seenPostIds, Map<String, Double> contentScores) {
        double social      = following.contains(post.getAuthorId()) ? 1.0 : 0.0;
        double collaborative = seenPostIds.contains(post.getId())   ? 0.0 : 0.5; // unfiltered placeholder
        double content     = contentScores.getOrDefault(post.getId(), 0.0);
        double trending    = trendingIds.contains(post.getId())     ? 1.0 : 0.0;

        return scoringService.calculateFeedScore(social, collaborative, content, trending, post.getCreatedAt());
    }

    /**
     * Normalises category_preferences scores to [0, 1] per post.
     */
    private Map<String, Double> computeContentScores(String userId, List<PostDoc> candidates) {
        try {
            String prefKey = "user:" + userId + ":category_preferences";
            Map<Object, Object> prefs = redisTemplate.opsForHash().entries(prefKey);
            if (prefs.isEmpty()) return Collections.emptyMap();

            double maxScore = prefs.values().stream()
                    .mapToLong(this::toLong).max().orElse(1L);

            Map<String, Double> scores = new HashMap<>();
            for (PostDoc post : candidates) {
                double score = 0.0;
                if (post.getCategory() != null) {
                    long catScore = toLong(prefs.get(post.getCategory()));
                    score = Math.max(score, catScore / maxScore);
                }
                if (post.getTags() != null) {
                    for (String tag : post.getTags()) {
                        long tagScore = toLong(prefs.get("tag:" + tag));
                        score = Math.max(score, tagScore / maxScore);
                    }
                }
                scores.put(post.getId(), Math.min(score, 1.0));
            }
            return scores;
        } catch (Exception e) {
            log.warn("Failed to compute content scores for user {}: {}", userId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Set<String> getTrendingPostIds(int windowHours, int limit) {
        try {
            String windowStart = Instant.now().minus(windowHours, ChronoUnit.HOURS).toString();
            NativeQuery query = NativeQuery.builder()
                    .withMaxResults(0)
                    .withQuery(Query.of(q -> q.range(r ->
                            r.field("createdAt").gte(co.elastic.clients.json.JsonData.of(windowStart)))))
                    .withAggregation("trending", Aggregation.of(a -> a
                            .terms(t -> t.field("postId").size(limit))))
                    .build();

            SearchHits<PostAction> hits = elasticsearchOperations.search(query, PostAction.class);
            return new HashSet<>(extractTermsBucketKeys(hits, "trending"));
        } catch (Exception e) {
            log.warn("Failed to compute trending post ids: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    private Set<String> getUserInteractedPostIds(String userId) {
        try {
            return postActionRepository.findByUserId(userId).stream()
                    .map(PostAction::getPostId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Failed to get interacted post ids for user {}: {}", userId, e.getMessage());
            return Collections.emptySet();
        }
    }

    private List<String> findSimilarUsers(String userId, int limit) {
        try {
            Set<String> myPosts = getUserInteractedPostIds(userId);
            if (myPosts.isEmpty()) return Collections.emptyList();

            Map<String, Long> overlap = new HashMap<>();
            postActionRepository.findByPostIdIn(new ArrayList<>(myPosts)).stream()
                    .filter(a -> !a.getUserId().equals(userId))
                    .forEach(a -> overlap.merge(a.getUserId(), 1L, Long::sum));

            return overlap.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to find similar users for {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<PostDoc> fetchRecentPosts(int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            return postSearchRepository.findAll(pageable).getContent();
        } catch (Exception e) {
            log.warn("Failed to fetch recent posts: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Diversity guard: no more than MAX_AUTHOR_PER_PAGE posts from the same author.
     */
    private List<FeedResponse.RankedPost> applyDiversityGuard(List<FeedResponse.RankedPost> ranked, int size) {
        Map<String, Integer> authorCount = new HashMap<>();
        List<FeedResponse.RankedPost> result = new ArrayList<>();
        for (FeedResponse.RankedPost rp : ranked) {
            if (result.size() >= size) break;
            String authorId = rp.getPost().getAuthorId();
            int count = authorCount.getOrDefault(authorId, 0);
            if (count < MAX_AUTHOR_PER_PAGE) {
                result.add(rp);
                authorCount.put(authorId, count + 1);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractTermsBucketKeys(SearchHits<?> hits, String aggName) {
        if (hits.getAggregations() == null) return Collections.emptyList();
        try {
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();
            var aggregate = aggs.get(aggName).aggregation().getAggregate();
            List<String> keys = new ArrayList<>();
            if (aggregate.isSterms()) {
                aggregate.sterms().buckets().array().forEach(b -> keys.add(b.key().stringValue()));
            }
            return keys;
        } catch (Exception e) {
            log.warn("Failed to extract terms bucket keys for {}: {}", aggName, e.getMessage());
            return Collections.emptyList();
        }
    }

    private FeedResponse emptyFeed(int page) {
        return FeedResponse.builder()
                .posts(Collections.emptyList())
                .currentPage(page)
                .totalElements(0)
                .hasNext(false)
                .build();
    }

    private String toJsonArray(Set<String> ids) {
        return ids.stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private long toLong(Object value) {
        if (value instanceof Long l)    return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof String s)  {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }
}

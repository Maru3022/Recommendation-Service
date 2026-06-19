package com.example.recommendationservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    private static final int MAX_VIEWED_POSTS = 500;

    @Id
    private String userId;

    @Column(columnDefinition = "TEXT")
    private String interestEmbeddingJson;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_following", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "following_id")
    private List<String> followingIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_liked_posts", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "post_id")
    private Set<String> likedPostIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_saved_posts", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "post_id")
    private Set<String> savedPostIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_viewed_posts", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "post_id")
    private List<String> viewedPostIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_preferred_categories", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category")
    private List<String> preferredCategories = new ArrayList<>();

    private Instant updatedAt;

    public void addViewedPost(String postId) {
        if (viewedPostIds.contains(postId)) return;
        if (viewedPostIds.size() >= MAX_VIEWED_POSTS) {
            viewedPostIds.remove(0);
        }
        viewedPostIds.add(postId);
    }

    public boolean hasViewed(String postId) {
        return viewedPostIds.contains(postId);
    }
}
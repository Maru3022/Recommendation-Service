package com.example.recommendationservice.service;

import com.example.recommendationservice.model.PostDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostVisibilityPolicy {
    private final SocialGraphService socialGraphService;

    public boolean canView(PostDoc post, String viewerId) {
        if (post == null || viewerId == null) return false;
        if (viewerId.equals(post.getAuthorId())) return true;
        String visibility = post.getVisibility();
        if (visibility == null || "PUBLIC".equalsIgnoreCase(visibility)) return true;
        return "FOLLOWERS".equalsIgnoreCase(visibility)
                && socialGraphService.isFollowing(viewerId, post.getAuthorId());
    }

    public List<PostDoc> visibleTo(List<PostDoc> posts, String viewerId) {
        return posts.stream().filter(post -> canView(post, viewerId)).toList();
    }
}

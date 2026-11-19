package com.example.recommendationservice.service;

import com.example.recommendationservice.model.PostDoc;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostVisibilityPolicyTest {
    @Test void appliesPublicFollowersAndPrivateForTwoUsers() {
        var graph = mock(SocialGraphService.class);
        when(graph.isFollowing("follower", "author")).thenReturn(true);
        var policy = new PostVisibilityPolicy(graph);
        PostDoc publicPost = post("PUBLIC"), followers = post("FOLLOWERS"), privatePost = post("PRIVATE");
        assertEquals(List.of(publicPost, followers), policy.visibleTo(List.of(publicPost, followers, privatePost), "follower"));
        assertEquals(List.of(publicPost), policy.visibleTo(List.of(publicPost, followers, privatePost), "stranger"));
        assertEquals(3, policy.visibleTo(List.of(publicPost, followers, privatePost), "author").size());
    }
    private PostDoc post(String visibility) { PostDoc p = new PostDoc(); p.setAuthorId("author"); p.setVisibility(visibility); return p; }
}

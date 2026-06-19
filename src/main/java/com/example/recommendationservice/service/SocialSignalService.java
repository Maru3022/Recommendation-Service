package com.example.recommendationservice.service;

import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.model.UserProfile;
import com.example.recommendationservice.repository.PostSearchRepository;
import com.example.recommendationservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialSignalService {

    private final UserProfileRepository userProfileRepository;
    private final PostSearchRepository postSearchRepository;

    public List<PostDoc> getPostsFromFollowing(String userId, int limit, Set<String> excludeIds) {
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        if (profile == null || profile.getFollowingIds().isEmpty()) {
            return Collections.emptyList();
        }

        List<PostDoc> posts = postSearchRepository.findByAuthorIdIn(
                profile.getFollowingIds(),
                PageRequest.of(0, limit * 3)
        );

        return posts.stream()
                .filter(p -> !excludeIds.contains(p.getId()))
                .filter(p -> !profile.getViewedPostIds().contains(p.getId()))
                .limit(limit)
                .toList();
    }
}
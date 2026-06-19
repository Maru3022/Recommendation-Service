package com.example.recommendationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lightweight social-graph service backed by Redis Sets.
 *
 * <p>Keys used:
 * <ul>
 *   <li>{@code social:{userId}:following}  – Set of userIds this user follows</li>
 *   <li>{@code social:{userId}:followers}  – Set of userIds that follow this user</li>
 *   <li>{@code social:{userId}:muted}      – Set of userIds muted by this user (optional)</li>
 * </ul>
 * </p>
 *
 * <p><b>Extension point:</b> In future this can be extracted into a dedicated
 * Social-Graph-Service with its own persistence (e.g. Neo4j or PostgreSQL)
 * while this service continues to read from a cache warm-up.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialGraphService {

    private final RedisTemplate<String, Object> redisTemplate;

    // -------------------------------------------------------------------------
    // Follow / unfollow
    // -------------------------------------------------------------------------

    public void follow(String followerId, String targetId) {
        if (followerId.equals(targetId)) {
            throw new IllegalArgumentException("A user cannot follow themselves");
        }
        redisTemplate.opsForSet().add(followingKey(followerId), targetId);
        redisTemplate.opsForSet().add(followersKey(targetId),   followerId);
        log.debug("User {} now follows {}", followerId, targetId);
    }

    public void unfollow(String followerId, String targetId) {
        redisTemplate.opsForSet().remove(followingKey(followerId), targetId);
        redisTemplate.opsForSet().remove(followersKey(targetId),   followerId);
        log.debug("User {} unfollowed {}", followerId, targetId);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public Set<String> getFollowing(String userId) {
        return toStringSet(redisTemplate.opsForSet().members(followingKey(userId)));
    }

    public Set<String> getFollowers(String userId) {
        return toStringSet(redisTemplate.opsForSet().members(followersKey(userId)));
    }

    /**
     * Returns the set of userIds muted by this user.
     * Posts from muted users are filtered out of the feed.
     */
    public Set<String> getMuted(String userId) {
        return toStringSet(redisTemplate.opsForSet().members(mutedKey(userId)));
    }

    public boolean isFollowing(String followerId, String targetId) {
        Boolean member = redisTemplate.opsForSet().isMember(followingKey(followerId), targetId);
        return Boolean.TRUE.equals(member);
    }

    // -------------------------------------------------------------------------

    private String followingKey(String userId) { return "social:" + userId + ":following"; }
    private String followersKey(String userId)  { return "social:" + userId + ":followers";  }
    private String mutedKey(String userId)      { return "social:" + userId + ":muted";      }

    @SuppressWarnings("unchecked")
    private Set<String> toStringSet(Set<Object> raw) {
        if (raw == null) return Collections.emptySet();
        return raw.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}

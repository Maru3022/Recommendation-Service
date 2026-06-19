package com.example.recommendationservice.service;

import com.example.recommendationservice.model.PostAction;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.repository.PostActionRepository;
import com.example.recommendationservice.repository.PostSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostActionServiceTest {

    @Mock private PostActionRepository postActionRepository;
    @Mock private PostSearchRepository postSearchRepository;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PostActionService postActionService;

    private PostAction postAction;

    @BeforeEach
    void setUp() {
        postAction = PostAction.builder()
                .id(1L)
                .userId("user1")
                .postId("post1")
                .actionType(PostAction.ActionType.LIKE)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void trackAction_savesAction() {
        when(postActionRepository.save(any(PostAction.class))).thenReturn(postAction);

        PostAction result = postActionService.trackAction("user1", "post1", "LIKE");

        assertThat(result.getUserId()).isEqualTo("user1");
        assertThat(result.getActionType()).isEqualTo(PostAction.ActionType.LIKE);
        verify(postActionRepository).save(any(PostAction.class));
    }

    @Test
    void trackAction_likeUpdatesCategoryPreferences() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        PostDoc post = new PostDoc();
        post.setId("post1");
        post.setCategory("FITNESS");
        post.setTags(List.of("workout"));
        post.setEmbedding(new float[1536]);

        when(postSearchRepository.findById("post1")).thenReturn(Optional.of(post));
        when(postActionRepository.save(any(PostAction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postActionService.trackAction("user1", "post1", "LIKE");

        verify(hashOperations).increment("user:user1:category_preferences", "FITNESS", 3L);
        verify(valueOperations).set(eq("user:user1:interest_embedding"), any(float[].class));
    }

    @Test
    void getActionsByPostId_returnsActions() {
        when(postActionRepository.findByPostId("post1")).thenReturn(List.of(postAction));

        List<PostAction> result = postActionService.getActionsByPostId("post1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPostId()).isEqualTo("post1");
    }

    @Test
    void getUserActionHistory_returnsActions() {
        when(postActionRepository.findByUserId("user1")).thenReturn(List.of(postAction));

        List<PostAction> result = postActionService.getUserActionHistory("user1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user1");
    }

    @Test
    void getActionById_returnsAction() {
        when(postActionRepository.findById(1L)).thenReturn(Optional.of(postAction));

        Optional<PostAction> result = postActionService.getActionById("1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }
}

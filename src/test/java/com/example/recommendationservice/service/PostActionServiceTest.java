package com.example.recommendationservice.service;

import com.example.recommendationservice.model.PostAction;
import com.example.recommendationservice.repository.PostActionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostActionServiceTest {

    @Mock private PostActionRepository postActionRepository;
    @Mock private UserProfileService userProfileService;

    @InjectMocks
    private PostActionService postActionService;

    private PostAction postAction;

    @BeforeEach
    void setUp() {
        postAction = new PostAction();
        postAction.setId("action1");
        postAction.setUserId("user1");
        postAction.setPostId("post1");
        postAction.setActionType("LIKE");
        postAction.setCreatedAt(Instant.now());
    }

    @Test
    void trackAction_savesAction() {
        when(postActionRepository.save(any(PostAction.class))).thenReturn(postAction);

        PostAction result = postActionService.trackAction("user1", "post1", "LIKE");

        assertThat(result.getUserId()).isEqualTo("user1");
        assertThat(result.getActionType()).isEqualTo("LIKE");
        verify(postActionRepository).save(any(PostAction.class));
    }

    @Test
    void trackAction_updatesUserProfileForLike() {
        when(postActionRepository.save(any(PostAction.class))).thenReturn(postAction);

        postActionService.trackAction("user1", "post1", "LIKE");

        verify(userProfileService).recordLike("user1", "post1");
    }

    @Test
    void trackAction_updatesUserProfileForSave() {
        when(postActionRepository.save(any(PostAction.class))).thenReturn(postAction);

        postActionService.trackAction("user1", "post1", "SAVE");

        verify(userProfileService).recordSave("user1", "post1");
    }

    @Test
    void trackAction_updatesUserProfileForView() {
        when(postActionRepository.save(any(PostAction.class))).thenReturn(postAction);

        postActionService.trackAction("user1", "post1", "VIEW");

        verify(userProfileService).recordView("user1", "post1");
    }

    @Test
    void getActionsByPostId_returnsActions() {
        when(postActionRepository.findByPostId("post1")).thenReturn(List.of(postAction));

        List<PostAction> result = postActionService.getActionsByPostId("post1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPostId()).isEqualTo("post1");
    }

    @Test
    void getActionsByUserId_returnsActions() {
        when(postActionRepository.findByUserId("user1")).thenReturn(List.of(postAction));

        List<PostAction> result = postActionService.getActionsByUserId("user1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("user1");
    }

    @Test
    void getActionById_returnsAction() {
        when(postActionRepository.findById("action1")).thenReturn(Optional.of(postAction));

        Optional<PostAction> result = postActionService.getActionById("action1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("action1");
    }
}

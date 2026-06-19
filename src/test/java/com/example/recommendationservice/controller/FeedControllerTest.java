package com.example.recommendationservice.controller;

import com.example.recommendationservice.dto.FeedRequest;
import com.example.recommendationservice.dto.FeedResponse;
import com.example.recommendationservice.service.FeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedControllerTest {

    @Mock private FeedService feedService;

    @InjectMocks
    private FeedController feedController;

    private FeedRequest feedRequest;

    @BeforeEach
    void setUp() {
        feedRequest = new FeedRequest();
        feedRequest.setUserId("user1");
        feedRequest.setPage(0);
        feedRequest.setSize(10);
        feedRequest.setExcludePostIds(Set.of());
    }

    @Test
    void getFeed_returnsFeedResponse() {
        FeedResponse expectedResponse = FeedResponse.builder()
                .page(0)
                .size(10)
                .hasMore(true)
                .build();
        when(feedService.getFeed(any(FeedRequest.class))).thenReturn(expectedResponse);

        ResponseEntity<FeedResponse> result = feedController.getFeed(feedRequest);

        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getPage()).isEqualTo(0);
    }

    @Test
    void invalidateCache_returnsOk() {
        ResponseEntity<Void> result = feedController.invalidateCache("user1");

        assertThat(result.getStatusCodeValue()).isEqualTo(200);
    }
}

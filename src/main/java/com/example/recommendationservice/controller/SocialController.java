package com.example.recommendationservice.controller;

import com.example.recommendationservice.service.SocialGraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * REST interface for the lightweight Redis-backed social graph.
 *
 * <p><b>Extension point:</b> in future this can delegate to a dedicated
 * Social-Graph-Service when the scale requires it.</p>
 */
@RestController
@RequestMapping("/api/social")
@Validated
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
@Tag(name = "Social Graph API", description = "Follow / unfollow and social graph queries")
public class SocialController {

    private final SocialGraphService socialGraphService;

    @Operation(summary = "Follow a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Now following"),
            @ApiResponse(responseCode = "400", description = "Cannot follow self")
    })
    @PostMapping("/{userId}/follow/{targetId}")
    public ResponseEntity<Void> follow(
            @PathVariable @NotBlank @Size(max = 50) String userId,
            @PathVariable @NotBlank @Size(max = 50) String targetId) {

        log.info("Follow | {} → {}", userId, targetId);
        socialGraphService.follow(userId, targetId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Unfollow a user")
    @ApiResponse(responseCode = "204", description = "Unfollowed")
    @DeleteMapping("/{userId}/follow/{targetId}")
    public ResponseEntity<Void> unfollow(
            @PathVariable @NotBlank @Size(max = 50) String userId,
            @PathVariable @NotBlank @Size(max = 50) String targetId) {

        log.info("Unfollow | {} → {}", userId, targetId);
        socialGraphService.unfollow(userId, targetId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get users that userId is following")
    @ApiResponse(responseCode = "200", description = "Following set returned")
    @GetMapping("/{userId}/following")
    public ResponseEntity<Set<String>> getFollowing(
            @PathVariable @NotBlank @Size(max = 50) String userId) {

        return ResponseEntity.ok(socialGraphService.getFollowing(userId));
    }

    @Operation(summary = "Get users that follow userId")
    @ApiResponse(responseCode = "200", description = "Followers set returned")
    @GetMapping("/{userId}/followers")
    public ResponseEntity<Set<String>> getFollowers(
            @PathVariable @NotBlank @Size(max = 50) String userId) {

        return ResponseEntity.ok(socialGraphService.getFollowers(userId));
    }
}

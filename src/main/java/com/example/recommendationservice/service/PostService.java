package com.example.recommendationservice.service;

import com.example.recommendationservice.model.CreatePostRequest;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles creation, retrieval, and deletion of posts.
 * Embedding generation is best-effort: if OpenAI is unavailable the post
 * is still saved without a vector (semantic search will simply miss it).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostSearchRepository postSearchRepository;
    private final EmbeddingService embeddingService;

    // -------------------------------------------------------------------------

    public PostDoc createPost(CreatePostRequest req) {
        PostDoc post = PostDoc.builder()
                .id(UUID.randomUUID().toString())
                .authorId(req.getAuthorId())
                .text(req.getText())
                .postType(req.getPostType())
                .tags(req.getTags() != null ? req.getTags() : new ArrayList<>())
                .category(req.getCategory())
                .visibility(req.getVisibility())
                .likesCount(0)
                .commentsCount(0)
                .sharesCount(0)
                .savesCount(0)
                .viewsCount(0)
                .createdAt(Instant.now())
                .build();

        // Generate embedding (best-effort)
        embeddingService.generateEmbeddingForPost(post)
                .ifPresent(post::setEmbedding);

        PostDoc saved = postSearchRepository.save(post);
        log.info("Created post {} by author {}", saved.getId(), saved.getAuthorId());
        return saved;
    }

    /**
     * Saves a post that already has all fields set (used by Kafka consumers).
     */
    public PostDoc savePost(PostDoc post) {
        if (post.getId() == null || post.getId().isBlank()) {
            post.setId(UUID.randomUUID().toString());
        }
        if (post.getCreatedAt() == null) {
            post.setCreatedAt(Instant.now());
        }
        embeddingService.generateEmbeddingForPost(post)
                .ifPresent(post::setEmbedding);

        PostDoc saved = postSearchRepository.save(post);
        log.debug("Saved post {} to Elasticsearch", saved.getId());
        return saved;
    }

    public Optional<PostDoc> getPost(String postId) {
        return postSearchRepository.findById(postId);
    }

    /**
     * Deletes the post only if the requestingUserId is the author.
     *
     * @return {@code true} if deleted, {@code false} if not found or not authorised
     */
    public boolean deletePost(String postId, String requestingUserId) {
        return postSearchRepository.findById(postId)
                .filter(p -> p.getAuthorId().equals(requestingUserId))
                .map(p -> {
                    postSearchRepository.deleteById(postId);
                    log.info("Deleted post {} by author {}", postId, requestingUserId);
                    return true;
                })
                .orElse(false);
    }
}

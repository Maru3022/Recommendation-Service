package com.example.recommendationservice.kafka;

import com.example.recommendationservice.dto.PostCreatedEvent;
import com.example.recommendationservice.model.PostDoc;
import com.example.recommendationservice.repository.PostSearchRepository;
import com.example.recommendationservice.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostCreatedConsumer {

    private final PostSearchRepository postSearchRepository;
    private final EmbeddingService embeddingService;

    @KafkaListener(
            topics = "post-created",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "postCreatedKafkaListenerContainerFactory"
    )
    public void consume(PostCreatedEvent event) {
        try {
            if (postSearchRepository.existsById(event.getPostId())) {
                log.debug("Post {} already indexed, skipping", event.getPostId());
                return;
            }

            PostDoc doc = new PostDoc();
            doc.setId(event.getPostId());
            doc.setAuthorId(event.getAuthorId());
            doc.setText(event.getText());
            doc.setTags(event.getTags());
            doc.setCategory(event.getCategory());
            doc.setPostType(event.getPostType());
            doc.setCreatedAt(event.getCreatedAt());

            Optional<float[]> embedding = embeddingService.generateEmbeddingForPost(doc);
            embedding.ifPresent(doc::setEmbedding);

            postSearchRepository.save(doc);
            log.debug("Indexed post {} in Elasticsearch", event.getPostId());
        } catch (Exception e) {
            log.error("Failed to process post-created event for post {}: {}", event.getPostId(), e.getMessage());
        }
    }
}
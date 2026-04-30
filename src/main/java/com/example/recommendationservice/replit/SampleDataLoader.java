package com.example.recommendationservice.replit;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.model.UserAction;
import com.example.recommendationservice.repository.ActionRepository;
import com.example.recommendationservice.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Seeds a small product catalog and a few user actions on startup so that the
 * Replit demo has visible data the moment the frontend connects. Active only
 * in the {@code replit} profile.
 */
@Component
@Profile("replit")
@RequiredArgsConstructor
@Slf4j
public class SampleDataLoader implements CommandLineRunner {

    private final ProductSearchRepository productRepository;
    private final ActionRepository actionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void run(String... args) {
        log.info("Loading sample data for replit profile...");

        List<ProductDoc> products = Arrays.asList(
                product("p-electronics-1", "Wireless Noise-Cancelling Headphones", "Electronics", 249.99,
                        "https://images.unsplash.com/photo-1518445930136-3f6c5af1f7e0?w=400"),
                product("p-electronics-2", "Smart 4K OLED TV 55\"", "Electronics", 1199.00,
                        "https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?w=400"),
                product("p-electronics-3", "Mechanical Keyboard RGB", "Electronics", 129.50,
                        "https://images.unsplash.com/photo-1561112078-7d24e04c3407?w=400"),
                product("p-electronics-4", "Mirrorless Camera 24MP", "Electronics", 899.00,
                        "https://images.unsplash.com/photo-1502920917128-1aa500764cbd?w=400"),

                product("p-books-1", "Designing Data-Intensive Applications", "Books", 42.00,
                        "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400"),
                product("p-books-2", "Clean Code", "Books", 35.50,
                        "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=400"),

                product("p-fashion-1", "Premium Cotton T-Shirt", "Fashion", 29.99,
                        "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400"),
                product("p-fashion-2", "Denim Jacket", "Fashion", 89.00,
                        "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=400")
        );

        productRepository.saveAll(products);
        log.info("Loaded {} sample products", products.size());

        // Sample user actions
        List<UserAction> actions = Arrays.asList(
                action("user-1", "p-electronics-1", "view"),
                action("user-1", "p-electronics-1", "like"),
                action("user-1", "p-books-1", "view"),
                action("user-2", "p-electronics-1", "view"),
                action("user-2", "p-electronics-2", "view"),
                action("user-2", "p-fashion-1", "like")
        );

        actions.forEach(actionRepository::save);
        log.info("Loaded {} sample user actions", actions.size());

        // Set user preferences in Redis
        redisTemplate.opsForValue().set("user:user-1:fav_category", "Electronics");
        redisTemplate.opsForValue().set("user:user-2:fav_category", "Electronics");
        log.info("Set sample user preferences");

        log.info("Sample data loading complete!");
    }

    private ProductDoc product(String id, String name, String category, double price, String imageUrl) {
        return new ProductDoc(id, name, category, price, imageUrl);
    }

    private UserAction action(String userId, String productId, String actionType) {
        return new UserAction(UUID.randomUUID().toString(), userId, productId, actionType);
    }
}

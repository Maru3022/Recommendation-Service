package com.example.recommendationservice.replit;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process replacement for {@link RedisTemplate} used in the {@code replit}
 * Spring profile. The production code calls a small subset of operations
 * (mostly {@code opsForValue().get/set} and {@code opsForHash().entries/increment})
 * on {@code RedisTemplate<String, Object>}; we back those with concurrent maps
 * and route any other method to a sensible default via {@link Proxy}.
 *
 * <p>This class deliberately does NOT require a {@code RedisConnectionFactory}
 * — {@link #afterPropertiesSet()} is a no-op so Spring does not error out
 * when the Redis auto-configuration is excluded.</p>
 */
public class InMemoryRedisTemplate extends RedisTemplate<String, Object> {

    private final Map<String, Object> values = new ConcurrentHashMap<>();
    private final Map<String, Map<Object, Long>> hashes = new ConcurrentHashMap<>();

    @Override
    public void afterPropertiesSet() {
        // no-op: we do not need a real RedisConnectionFactory in the replit profile
    }

    @SuppressWarnings("unchecked")
    @Override
    public ValueOperations<String, Object> opsForValue() {
        return (ValueOperations<String, Object>) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{ValueOperations.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "get":
                            return values.get((String) args[0]);
                        case "set":
                            values.put((String) args[0], args[1]);
                            return null;
                        case "putIfAbsent":
                            return values.putIfAbsent((String) args[0], args[1]);
                        default:
                            return null;
                    }
                }
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public HashOperations<String, Object, Object> opsForHash() {
        return (HashOperations<String, Object, Object>) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HashOperations.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    switch (name) {
                        case "entries":
                            String key = (String) args[0];
                            return (Map<Object, Object>) hashes.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
                        case "put":
                            String putKey = (String) args[0];
                            hashes.computeIfAbsent(putKey, k -> new ConcurrentHashMap<>())
                                    .put((Object) args[1], (Long) args[2]);
                            return null;
                        case "increment":
                            String incKey = (String) args[0];
                            Map<Object, Long> hash = hashes.computeIfAbsent(incKey, k -> new ConcurrentHashMap<>());
                            Object hashKey = args[1];
                            long delta = args[2] instanceof Long ? (Long) args[2] : Long.parseLong(args[2].toString());
                            hash.merge((Object) hashKey, delta, Long::sum);
                            return hash.get(hashKey);
                        case "get":
                            String getKey = (String) args[0];
                            Map<Object, Long> getHash = hashes.get(getKey);
                            return getHash != null ? getHash.get(args[1]) : null;
                        case "hasKey":
                            String hasKey = (String) args[0];
                            Map<Object, Long> hasHash = hashes.get(hasKey);
                            return hasHash != null && hasHash.containsKey(args[1]);
                        case "delete":
                            String delKey = (String) args[0];
                            Map<Object, Long> delHash = hashes.get(delKey);
                            if (delHash != null) {
                                delHash.remove(args[1]);
                                return 1L;
                            }
                            return 0L;
                        default:
                            return null;
                    }
                }
        );
    }
}

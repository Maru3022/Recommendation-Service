package com.example.recommendationservice.replit;

import com.example.recommendationservice.model.UserAction;
import com.example.recommendationservice.repository.ActionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link ActionRepository} for the {@code replit}
 * profile. Backed by a {@link ConcurrentHashMap}; only the methods actually
 * used by the service layer ({@link #findByUserId}, {@link #findByProductId},
 * {@link #save}, {@link #findAll}) are implemented; the remaining
 * {@link org.springframework.data.elasticsearch.repository.ElasticsearchRepository}
 * methods throw {@link UnsupportedOperationException}.
 */
@Component
@Profile("replit")
public class InMemoryActionRepository implements ActionRepository {

    private final Map<String, UserAction> store = new ConcurrentHashMap<>();

    @Override
    public List<UserAction> findByUserId(String userId) {
        return store.values().stream()
                .filter(a -> userId != null && userId.equals(a.getUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserAction> findByProductId(String productId) {
        return store.values().stream()
                .filter(a -> productId != null && productId.equals(a.getProductId()))
                .collect(Collectors.toList());
    }

    @Override
    public UserAction save(UserAction action) {
        if (action.getId() == null || action.getId().isEmpty()) {
            action.setId(java.util.UUID.randomUUID().toString());
        }
        store.put(action.getId(), action);
        return action;
    }

    @Override
    public Optional<UserAction> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsById(String id) {
        return store.containsKey(id);
    }

    @Override
    public List<UserAction> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }

    @Override
    public void deleteAll() {
        store.clear();
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void delete(UserAction entity) {
        store.remove(entity.getId());
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        ids.forEach(store::remove);
    }

    @Override
    public Page<UserAction> findAll(Pageable pageable) {
        List<UserAction> all = new ArrayList<>(store.values());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        if (start >= all.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, all.size());
        }
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }

    @Override
    public List<UserAction> findAll(Sort sort) {
        return new ArrayList<>(store.values());
    }

    @Override
    public void refresh() {
        // no-op for in-memory
    }

    @Override
    public void setRefreshPolicy(RefreshPolicy refreshPolicy) {
        // no-op for in-memory
    }
}

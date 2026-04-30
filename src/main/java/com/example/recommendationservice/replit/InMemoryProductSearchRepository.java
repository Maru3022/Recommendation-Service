package com.example.recommendationservice.replit;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.repository.ProductSearchRepository;
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
 * In-memory implementation of {@link ProductSearchRepository} used in the
 * {@code replit} profile, where Spring Data Elasticsearch repositories are
 * disabled. Only the methods that production code actually calls are
 * implemented; the rest of the {@link org.springframework.data.elasticsearch.repository.ElasticsearchRepository}
 * surface throws {@link UnsupportedOperationException} so that any unexpected
 * usage fails fast and loudly.
 */
@Component
@Profile("replit")
public class InMemoryProductSearchRepository implements ProductSearchRepository {

    private final Map<String, ProductDoc> store = new ConcurrentHashMap<>();

    @Override
    public Page<ProductDoc> findByCategory(String category, Pageable pageable) {
        List<ProductDoc> matches = store.values().stream()
                .filter(p -> p.getCategory() != null && p.getCategory().equalsIgnoreCase(category))
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .collect(Collectors.toList());
        return paginate(matches, pageable);
    }

    @Override
    public List<ProductDoc> findAllByIdIn(List<String> ids) {
        return ids.stream()
                .map(store::get)
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }

    @Override
    public ProductDoc save(ProductDoc product) {
        store.put(product.getId(), product);
        return product;
    }

    @Override
    public void saveAll(List<ProductDoc> products) {
        products.forEach(p -> store.put(p.getId(), p));
    }

    @Override
    public Optional<ProductDoc> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsById(String id) {
        return store.containsKey(id);
    }

    @Override
    public List<ProductDoc> findAll() {
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
    public void delete(ProductDoc entity) {
        store.remove(entity.getId());
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        ids.forEach(store::remove);
    }

    @Override
    public Page<ProductDoc> findAll(Pageable pageable) {
        return paginate(new ArrayList<>(store.values()), pageable);
    }

    @Override
    public List<ProductDoc> findAll(Sort sort) {
        List<ProductDoc> all = new ArrayList<>(store.values());
        all.sort((a, b) -> {
            for (Sort.Order order : sort) {
                int cmp;
                if ("id".equals(order.getProperty())) {
                    cmp = a.getId().compareTo(b.getId());
                } else if ("name".equals(order.getProperty())) {
                    cmp = a.getName().compareTo(b.getName());
                } else if ("price".equals(order.getProperty())) {
                    cmp = Double.compare(a.getPrice(), b.getPrice());
                } else {
                    cmp = 0;
                }
                if (cmp != 0) {
                    return order.isAscending() ? cmp : -cmp;
                }
            }
            return 0;
        });
        return all;
    }

    @Override
    public void refresh() {
        // no-op for in-memory
    }

    @Override
    public void setRefreshPolicy(RefreshPolicy refreshPolicy) {
        // no-op for in-memory
    }

    private Page<ProductDoc> paginate(List<ProductDoc> all, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        if (start >= all.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, all.size());
        }
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }
}

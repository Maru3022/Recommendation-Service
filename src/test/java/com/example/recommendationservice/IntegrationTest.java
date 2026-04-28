package com.example.recommendationservice;

import com.example.recommendationservice.model.ProductDoc;
import com.example.recommendationservice.model.UserAction;
import com.example.recommendationservice.repository.ActionRepository;
import com.example.recommendationservice.repository.ProductSearchRepository;
import com.example.recommendationservice.service.UserActionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test disabled by default - requires full infrastructure (Elasticsearch, Redis, PostgreSQL)
 * Run manually when integration test infrastructure is available
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Disabled("Requires Elasticsearch, Redis, and PostgreSQL - disabled for CI/CD")
public class IntegrationTest {

    @MockitoBean
    private ProductSearchRepository productRepository;

    @MockitoBean
    private ActionRepository actionRepository;

    @MockitoBean
    private UserActionService userActionService;

    @BeforeEach
    void setUp() {
        // Clean up repositories
        actionRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void testFullRecommendationFlow() {
        // 1. Create test products
        ProductDoc laptop = new ProductDoc("1", "Laptop", "Electronics", 999.0, "url1");
        ProductDoc book = new ProductDoc("2", "Book", "Books", 19.0, "url2");
        ProductDoc shirt = new ProductDoc("3", "Shirt", "Fashion", 49.0, "url3");

        productRepository.saveAll(Arrays.asList(laptop, book, shirt));

        // 2. Track user actions
        UserAction viewAction = new UserAction("action1", "user1", "1", "view");
        UserAction likeAction = new UserAction("action2", "user1", "1", "like");
        UserAction cartAction = new UserAction("action3", "user1", "2", "add_to_cart");

        userActionService.trackAction(viewAction);
        userActionService.trackAction(likeAction);
        userActionService.trackAction(cartAction);

        // 3. Verify actions are saved
        List<UserAction> userActions = (List<UserAction>) userActionService.getUserActionHistory("user1");
        assertEquals(3, userActions.size());

        // 4. Test product retrieval
        List<ProductDoc> electronics = productRepository.findByCategory("Electronics", PageRequest.of(0, 10)).getContent();
        assertEquals(1, electronics.size());
        assertEquals("Laptop", electronics.get(0).getName());

        // 5. Test product search by IDs
        List<ProductDoc> foundProducts = productRepository.findAllByIdIn(Arrays.asList("1", "2"));
        assertEquals(2, foundProducts.size());
    }

    @Test
    void testProductValidation() {
        // Test creating products with validation
        ProductDoc validProduct = new ProductDoc("valid-id", "Valid Product", "Electronics", 99.0, "url");
        ProductDoc invalidProduct = new ProductDoc("", "", "", -1.0, "");

        // This would be tested in controller layer with validation
        assertNotNull(validProduct.getId());
        assertNotNull(validProduct.getName());
        assertTrue(validProduct.getPrice() > 0);
    }

    @Test
    void testUserActionValidation() {
        // Test creating user actions with validation
        UserAction validAction = new UserAction("action1", "user1", "product1", "view");
        
        assertNotNull(validAction.getId());
        assertNotNull(validAction.getUserId());
        assertNotNull(validAction.getProductId());
        assertNotNull(validAction.getActionType());
    }

    @Test
    void testRepositoryOperations() {
        // Test basic repository operations
        ProductDoc product = new ProductDoc("test", "Test Product", "Test", 10.0, "test-url");
        productRepository.save(product);

        assertTrue(productRepository.existsById("test"));
        
        List<ProductDoc> found = productRepository.findAllByIdIn(Arrays.asList("test"));
        assertEquals(1, found.size());
        
        productRepository.deleteById("test");
        assertFalse(productRepository.existsById("test"));
    }
}

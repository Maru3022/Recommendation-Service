package com.example.recommendationservice.controller;

import com.example.recommendationservice.model.UserAction;
import com.example.recommendationservice.service.UserActionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@Slf4j
@RequestMapping("/api/user-actions")
@RequiredArgsConstructor
@Validated
public class UserActionController {

    private final UserActionService userActionService;

    @PostMapping
    public ResponseEntity<Void> trackUserAction(@Valid @RequestBody UserAction userAction) {
        log.info("Tracking user action: userId={}, productId={}, actionType={}", 
                userAction.getUserId(), userAction.getProductId(), userAction.getActionType());
        
        userActionService.trackAction(userAction);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/history")
    public ResponseEntity<Iterable<UserAction>> getUserActionHistory(@PathVariable String userId) {
        log.info("Fetching action history for userId: {}", userId);
        return ResponseEntity.ok(userActionService.getUserActionHistory(userId));
    }
}

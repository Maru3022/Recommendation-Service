package com.example.recommendationservice.repository;

import com.example.recommendationservice.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    @Query("SELECT u FROM UserProfile u JOIN u.followingIds f WHERE f = :userId")
    List<UserProfile> findFollowers(@Param("userId") String userId);
}
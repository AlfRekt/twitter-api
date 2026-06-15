package com.workintech.twitterapi.repository;

import com.workintech.twitterapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface IUserRepository extends JpaRepository<User,Long> {
    @Query("SELECT u FROM User u WHERE u.userName = :username")
    Optional<User> findUserByUsername(String username);

    boolean existsByEmail(String email);
}

package com.example.chat_app_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.chat_app_backend.models.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsername(String username);

    Optional<User> findById(Long id);

    List<User> findByIdNot(Long id);

}
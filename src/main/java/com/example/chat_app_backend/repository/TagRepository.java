package com.example.chat_app_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.chat_app_backend.models.Tag;
import java.util.*;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByOwnerId(Long ownerId);

    Optional<Tag> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndNameIgnoreCase(Long ownerId, String name);

    long countByOwnerId(Long ownerId);

}

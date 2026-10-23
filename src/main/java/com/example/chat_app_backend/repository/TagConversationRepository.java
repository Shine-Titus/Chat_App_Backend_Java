package com.example.chat_app_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.chat_app_backend.models.TagConversation;
import java.util.*;

public interface TagConversationRepository extends JpaRepository<TagConversation, Long> {

    List<TagConversation> findByTagId(Long tagId);

    List<TagConversation> findByTagIdIn(Collection<Long> tagIds);

    boolean existsByTagIdAndConversationId(Long tagId, Long conversationId);

    void deleteByTagId(Long tagId);

    void deleteByTagIdAndConversationId(Long tagId, Long conversationId);
}

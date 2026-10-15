package com.example.chat_app_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.chat_app_backend.models.Conversation;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByConversationId(Long conversationId);

}
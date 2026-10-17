package com.example.chat_app_backend.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.chat_app_backend.models.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByIdAsc(Long conversationId);

    List<Message> findByConversationIdAndCreatedAtBetweenOrderByIdAsc(Long conversationId, Instant from, Instant to);

}
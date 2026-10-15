package com.example.chat_app_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.chat_app_backend.models.Participant;
import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    boolean existsByConversationIdAndUserId(Long ConversationId, Long UserId);

    List<Participant> findByConversationId(Long conversationId);

    List<Participant> findByUserId(Long userId);

    Optional<Participant> findFirstByConversationIdAndUserIdNot(Long conversationId, Long userId);

}
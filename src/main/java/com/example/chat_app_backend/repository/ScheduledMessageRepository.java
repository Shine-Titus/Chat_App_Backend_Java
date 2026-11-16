package com.example.chat_app_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.chat_app_backend.models.ScheduledMessage;
import org.springframework.data.domain.Pageable;


import java.time.Instant;
import java.util.List;

public interface ScheduledMessageRepository extends JpaRepository<ScheduledMessage, Long> {
    
    List<ScheduledMessage> findByStatusAndSendAtLessThanEqualOrderBySendAtAsc(
        ScheduledMessage.Status status, Instant now, Pageable pageable);

    List<ScheduledMessage> findByConversationIdAndSenderIdAndStatus(Long conversationId, Long senderId, ScheduledMessage.Status status);

    void deleteById(Long id);
    
}
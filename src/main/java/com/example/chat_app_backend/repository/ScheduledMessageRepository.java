package com.example.chat_app_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.chat_app_backend.models.ScheduledMessage;

import java.time.Instant;
import java.util.List;

public interface ScheduledMessageRepository extends JpaRepository<ScheduledMessage, Long> {
    
    List<ScheduledMessage> findBySendAtLesserThanEqual(Instant sendAt);
    
}
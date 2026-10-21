package com.example.chat_app_backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.chat_app_backend.models.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
                SELECT COUNT(m) from Message m
                WHERE m.conversationId = :cid
                AND m.id > :lastRead
                AND m.senderId <> :me
                AND m.createdAt >= :from
                AND m.createdAt <= :to
            """)
    long countUnread(@Param("cid") Long cid, @Param("lastRead") Long lastRead,
            @Param("me") Long me, @Param("from") Instant from, @Param("to") Instant to);

    Optional<Message> findTopByConversationIdOrderByIdDesc(Long conversationId);

    List<Message> findByConversationIdOrderByIdAsc(Long conversationId);

    List<Message> findByConversationIdAndCreatedAtBetweenOrderByIdAsc(Long conversationId, Instant from, Instant to);

}
package com.example.chat_app_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.chat_app_backend.models.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            SELECT m FROM Message m
            WHERE (m.senderId = :me and m.receiverId = :otherId)
                OR (m.senderId = :otherId and m.receiverId = :me)
            ORDER BY m.id ASC
            """)
    List<Message> findConversation(@Param("me") Long me, @Param("otherId") Long otherId);

}
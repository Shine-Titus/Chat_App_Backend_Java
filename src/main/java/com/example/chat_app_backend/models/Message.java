package com.example.chat_app_backend.models;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_conversation_created", columnList = "conversation_id, created_at")
})
@Data
@NoArgsConstructor
public class Message {

    public enum Type {
        TEXT, FILE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conversationId;

    @Column(nullable = false)
    private Long senderId;

    private String content;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private Type type = Type.TEXT;

    private Long fileAttachmentId;  // Nullable, only set if type is FILE
}
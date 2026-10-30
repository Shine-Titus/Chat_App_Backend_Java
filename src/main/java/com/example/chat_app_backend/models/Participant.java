package com.example.chat_app_backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "participants", uniqueConstraints = @UniqueConstraint(columnNames = { "conversation_id", "user_id" }))
@Data
@NoArgsConstructor
public class Participant {

    public enum GetRole {
        ADMIN, MEMBER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conversationId;

    @Column(nullable = false)
    private Long userId;

    private Long lastReadMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GetRole role;

    private Instant addedAt;

    private Instant removedAt;

}
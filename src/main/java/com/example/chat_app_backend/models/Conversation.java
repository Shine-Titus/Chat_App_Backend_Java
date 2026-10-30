package com.example.chat_app_backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "conversations")
@Data
@NoArgsConstructor
public class Conversation {

    public enum GroupType {
        DIRECT, GROUP
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupType type;

    private String name;

    private String description;

    private Long createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

}
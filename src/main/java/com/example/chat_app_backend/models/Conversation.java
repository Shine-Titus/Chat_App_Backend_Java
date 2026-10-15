package com.example.chat_app_backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long conversationId;

    private String type;

    private String name;

    private Long createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

}
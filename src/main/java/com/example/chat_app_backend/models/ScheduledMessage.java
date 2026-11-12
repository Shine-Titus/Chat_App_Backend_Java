package com.example.chat_app_backend.models;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Data 
@Table(
    name = "scheduled_messages",
    indexes = @Index(name = "idx_scheduled_messages_status_send_at", columnList = "status, sendAt")
)
@NoArgsConstructor 
public class ScheduledMessage {

    public enum Status {
        PENDING, SENT, FAILED
    }

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conversationId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Instant sendAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column (nullable = false)
    private Status status;

}

package com.example.chat_app_backend.models;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.Id;

@Entity 
@Table(name = "files")
@Data 
public class FileAttachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalName;
    
    private String storedName;   // UUID-based, never the user's filename

    private String contentType;

    private long size;

    private Long uploaderId;

    private Long conversationId;

    private Instant uploadedAt = Instant.now();

}

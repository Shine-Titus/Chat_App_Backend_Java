package com.example.chat_app_backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Id;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "tags", uniqueConstraints = @UniqueConstraint(columnNames = { "ownerId", "name" }))
@Data
@NoArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 30)
    private String name;

    private String color;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

}

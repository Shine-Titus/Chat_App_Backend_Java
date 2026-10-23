package com.example.chat_app_backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tagconversations", uniqueConstraints = @UniqueConstraint(columnNames = { "tag_id", "conversation_id" }))
@Data
@NoArgsConstructor
public class TagConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tagId;

    @Column(nullable = false)
    private Long conversationId;
}

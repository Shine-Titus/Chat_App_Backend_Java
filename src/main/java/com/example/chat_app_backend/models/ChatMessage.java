package com.example.chat_app_backend.models;

public class ChatMessage {

    private Long id;
    private Long senderId;
    private Long conversationId;
    private String content;
    private String createdAt;

    public ChatMessage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public void setConversationid(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
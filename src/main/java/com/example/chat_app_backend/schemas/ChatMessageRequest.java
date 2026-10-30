package com.example.chat_app_backend.schemas;

import lombok.Data;

@Data
public class ChatMessageRequest {

    private Long id;
    private Long senderId;
    private Long conversationId;
    private String content;
    private String createdAt;

}
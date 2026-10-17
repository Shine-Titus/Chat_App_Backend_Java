package com.example.chat_app_backend.schemas;

import lombok.Data;

@Data
public class DirectConversationRequest {
    private Long userId;

    public Long getUserId() {
        return userId;
    }
}

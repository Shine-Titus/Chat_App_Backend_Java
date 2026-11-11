package com.example.chat_app_backend.schemas;
import lombok.Data;

@Data 
public class ScheduledMessageRequest {

    private Long conversationId;
    private String content;
    private Long sendAt;

}

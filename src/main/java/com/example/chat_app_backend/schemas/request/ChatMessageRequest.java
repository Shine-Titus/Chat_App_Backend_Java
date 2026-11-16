package com.example.chat_app_backend.schemas.request;

import lombok.Data;
import com.example.chat_app_backend.models.Message;
import com.example.chat_app_backend.dto.FileResponse;

@Data
public class ChatMessageRequest {

    private Long id;
    private Long senderId;
    private Long conversationId;
    private String content;
    private String createdAt;
    private Message.Type type;
    private Long fileAttachmentId;  // Nullable, only set if type is FILE
    private FileResponse fileAttachment;  // Nullable, only set if type is FILE

}
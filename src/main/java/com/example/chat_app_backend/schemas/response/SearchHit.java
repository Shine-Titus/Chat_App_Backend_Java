package com.example.chat_app_backend.schemas.response;

import java.time.*;

public record SearchHit(Long messageId, String snippet, String senderUsername, Instant createdAt) {
}

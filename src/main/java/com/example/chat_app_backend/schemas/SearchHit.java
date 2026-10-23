package com.example.chat_app_backend.schemas;

import java.time.*;

public record SearchHit(Long messageId, String snippet, String senderUsername, Instant createdAt) {
}

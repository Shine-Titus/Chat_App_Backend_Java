package com.example.chat_app_backend.schemas;

import java.util.*;
import lombok.Data;

@Data
public class TagConversationsRequest {
    private List<Long> conversationIds;
}

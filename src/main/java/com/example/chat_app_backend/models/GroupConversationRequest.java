package com.example.chat_app_backend.models;

import lombok.Data;
import java.util.*;

@Data
public class GroupConversationRequest {

    private String name;

    private List<Long> participantIds;
}

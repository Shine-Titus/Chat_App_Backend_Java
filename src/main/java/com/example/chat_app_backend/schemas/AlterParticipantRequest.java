package com.example.chat_app_backend.schemas;

import java.util.*;
import lombok.Data;

@Data
public class AlterParticipantRequest {
    private List<Long> participantUserIds;
}

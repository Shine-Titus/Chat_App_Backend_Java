package com.example.chat_app_backend.schemas;

import java.util.List;

public record SearchResult(Long conversationId, List<SearchHit> matches) {

}

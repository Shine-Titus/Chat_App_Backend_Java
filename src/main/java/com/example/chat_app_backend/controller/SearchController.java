package com.example.chat_app_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.chat_app_backend.models.User;
import com.example.chat_app_backend.models.Message;

import com.example.chat_app_backend.repository.ConversationRepository;
import com.example.chat_app_backend.repository.MessageRepository;
import com.example.chat_app_backend.repository.ParticipantRepository;
import com.example.chat_app_backend.repository.UserRepository;

import com.example.chat_app_backend.schemas.SearchHit;
import com.example.chat_app_backend.schemas.SearchResult;

import org.springframework.security.core.Authentication;
import java.util.*;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;

    public SearchController(MessageRepository messageRepository, ConversationRepository conversationRepository,
            ParticipantRepository participantRepository, UserRepository userRepository) {
        this.participantRepository = participantRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @GetMapping()
    public ResponseEntity<?> getMessageSearches(@RequestParam("q") String q, Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        String term = q.trim();

        List<Long> conversationIds = participantRepository.findByUserId(user.getId()).stream()
                .map(participation -> participation.getConversationId()).toList();

        if (term.length() < 2 || term.length() > 100) {
            return ResponseEntity.badRequest().body("Query length not within range.");
        }

        List<Message> messages = messageRepository.searchInConversations(conversationIds, term);

        Map<Long, String> names = new HashMap<>();
        for (User u : userRepository.findAllById(messages.stream().map(Message::getSenderId).distinct().toList())) {
            names.put(u.getId(), u.getUsername());
        }

        Map<Long, List<SearchHit>> grouped = new LinkedHashMap<>();
        for (Message m : messages) {
            List<SearchHit> hits = grouped.get(m.getConversationId());

            if (hits == null) {
                hits = new ArrayList<>();
                grouped.put(m.getConversationId(), hits);
            }

            if (hits.size() < 3) {
                hits.add(new SearchHit(m.getId(), m.getContent(), names.get(m.getSenderId()), m.getCreatedAt()));
            }

        }

        List<SearchResult> result = new ArrayList<>();
        for (Map.Entry<Long, List<SearchHit>> e : grouped.entrySet()) {
            result.add(new SearchResult(e.getKey(), e.getValue()));
        }

        return ResponseEntity.ok(result);

    }

}

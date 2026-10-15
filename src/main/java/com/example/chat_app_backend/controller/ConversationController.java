package com.example.chat_app_backend.controller;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.example.chat_app_backend.models.Conversation;
import com.example.chat_app_backend.models.Message;
import com.example.chat_app_backend.models.User;
import com.example.chat_app_backend.models.Participant;

import com.example.chat_app_backend.models.DirectConversationRequest;
import com.example.chat_app_backend.repository.ConversationRepository;
import com.example.chat_app_backend.models.GroupConversationRequest;
import com.example.chat_app_backend.repository.UserRepository;

import jakarta.transaction.Transactional;

import com.example.chat_app_backend.repository.MessageRepository;
import com.example.chat_app_backend.repository.ParticipantRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/conversations")
@CrossOrigin(origins = "http://localhost:5173")
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public ConversationController(ConversationRepository conversationRepository,
            ParticipantRepository participantRepository, MessageRepository messageRepository,
            UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    // get all conversations
    @GetMapping
    public List<Map<String, Object>> getConversations(Authentication authentication) {

        String myUsername = authentication.getName();

        User me = userRepository.findByUsername(myUsername).orElseThrow();

        List<Long> ids = participantRepository.findByUserId(me.getId()).stream()
                .map(Participant::getConversationId)
                .toList();

        return conversationRepository.findAllById(ids).stream()
                .map(c -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", c.getConversationId());
                    dto.put("type", c.getType());

                    if ("GROUP".equals(c.getType())) {
                        dto.put("name", c.getName());
                    } else {
                        Participant otherParticipant = participantRepository
                                .findFirstByConversationIdAndUserIdNot(c.getConversationId(), me.getId()).orElseThrow();
                        User otherUser = userRepository.findById(otherParticipant.getUserId()).orElseThrow();
                        dto.put("id", c.getConversationId());
                        dto.put("name", otherUser.getUsername());
                        dto.put("type", c.getType());
                        dto.put("otherUserId", otherUser.getId());
                        dto.put("otherUsername", otherUser.getUsername());
                    }
                    return dto;

                }).toList();
    }

    // open direct conversation, if not present create one
    @PostMapping("/direct")
    @Transactional
    public ResponseEntity<?> openDirectConversation(@RequestBody DirectConversationRequest request,
            Authentication authentication) {

        String myUsername = authentication.getName();
        User me = userRepository.findByUsername(myUsername).orElseThrow();
        Long otherId = request.getUserId();

        if (otherId == null || otherId.equals(me.getId())) {
            return ResponseEntity.badRequest().body(otherId);
        }

        User other = userRepository.findById(otherId).orElse(null);
        if (other == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        List<Long> myIds = participantRepository.findByUserId(me.getId()).stream()
                .map(Participant::getConversationId)
                .toList();

        Conversation conversation = conversationRepository.findAllById(myIds).stream()
                .filter(c -> "DIRECT".equals(c.getType()))
                .filter(c -> participantRepository.existsByConversationIdAndUserId(c.getConversationId(), otherId))
                .findFirst()
                .orElseGet(() -> {
                    Conversation created = new Conversation();
                    created.setType("DIRECT");
                    created.setCreatedBy(me.getId());
                    created = conversationRepository.save(created);

                    Participant p1 = new Participant();
                    p1.setConversationId(created.getConversationId());
                    p1.setUserId(me.getId());

                    Participant p2 = new Participant();
                    p2.setConversationId(created.getConversationId());
                    p2.setUserId(otherId);

                    participantRepository.save(p1);
                    participantRepository.save(p2);
                    return created;
                });

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", conversation.getConversationId());
        dto.put("name", conversation.getName());
        dto.put("type", conversation.getType());
        dto.put("otherUserId", other.getId());
        dto.put("otherUsername", other.getUsername());
        return ResponseEntity.ok(dto);
    }

    // load messages for any conversation regardless of type
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<?> loadMessages(@PathVariable Long conversationId, Authentication authentication) {
        String myUsername = authentication.getName();
        User me = userRepository.findByUsername(myUsername).orElseThrow();

        if (!participantRepository.existsByConversationIdAndUserId(conversationId, me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not a participant");
        }

        List<Message> messages = messageRepository.findByConversationIdOrderByIdAsc(conversationId);

        Map<Long, String> names = userRepository
                .findAllById(messages.stream().map(Message::getSenderId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        List<Map<String, Object>> result = messages.stream().map(m -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", m.getId());
            dto.put("conversationId", m.getConversationId());
            dto.put("senderId", m.getSenderId());
            dto.put("senderUsername", names.get(m.getSenderId()));
            dto.put("content", m.getContent());
            dto.put("createdAt", m.getCreatedAt());
            return dto;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping()
    @Transactional
    public ResponseEntity<?> createGroupChat(@RequestBody GroupConversationRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        User me = userRepository.findByUsername(username).orElseThrow();

        Conversation created = new Conversation();
        created.setType("GROUP");
        created.setName(request.getName());
        created.setCreatedBy(me.getId());
        created = conversationRepository.save(created);

        Participant p1 = new Participant();
        p1.setConversationId(created.getConversationId());
        p1.setUserId(me.getId());
        participantRepository.save(p1);

        for (Long userId : request.getParticipantIds()) {
            Participant p = new Participant();
            p.setConversationId(created.getConversationId());
            p.setUserId(userId);
            participantRepository.save(p);
        }

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", created.getConversationId());
        dto.put("type", created.getType());
        dto.put("name", created.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

}

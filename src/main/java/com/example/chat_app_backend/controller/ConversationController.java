package com.example.chat_app_backend.controller;

import java.util.*;
import java.util.stream.Collectors;
import java.time.Instant;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.example.chat_app_backend.models.Conversation;
import com.example.chat_app_backend.models.Message;
import com.example.chat_app_backend.models.User;
import com.example.chat_app_backend.models.Participant;
import com.example.chat_app_backend.repository.ConversationRepository;
import com.example.chat_app_backend.repository.UserRepository;
import com.example.chat_app_backend.schemas.AlterParticipantRequest;
import com.example.chat_app_backend.schemas.DirectConversationRequest;
import com.example.chat_app_backend.schemas.GroupConversationRequest;

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

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        // List<Participant> mine = participantRepository.findByUserId(user.getId());

        List<Long> ids = participantRepository.findByUserId(user.getId()).stream()
                .map(Participant::getConversationId)
                .toList();

        return conversationRepository.findAllById(ids).stream()
                .map(c -> {

                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", c.getConversationId());
                    dto.put("type", c.getType());

                    Participant mine = participantRepository
                            .findByConversationIdAndUserId(c.getConversationId(), user.getId()).orElse(null);

                    // dto.put("active", mine.getRemovedAt());

                    // sending un read messages
                    Participant me = participantRepository
                            .findByConversationIdAndUserId(c.getConversationId(), user.getId()).orElseThrow();
                    Instant from = mine.getAddedAt() != null ? mine.getAddedAt() : Instant.EPOCH;
                    Instant to = mine.getRemovedAt() != null ? mine.getRemovedAt() : Instant.now();
                    long lastRead = mine.getLastReadMessageId() == null ? 0L : mine.getLastReadMessageId();

                    dto.put("unreadCount", messageRepository.countUnread(c.getConversationId(), lastRead,
                            me.getUserId(), from, to));

                    dto.put("createdAt", c.getCreatedAt());
                    dto.put("description", c.getDescription());

                    if (mine.getRemovedAt() == null) {
                        dto.put("isActive", true);
                    } else {
                        dto.put("isActive", false);
                    }

                    if ("GROUP".equals(c.getType())) {
                        dto.put("name", c.getName());
                    } else {
                        Participant otherParticipant = participantRepository
                                .findFirstByConversationIdAndUserIdNot(c.getConversationId(), user.getId());

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
                    created.setType(Conversation.GroupType.DIRECT);
                    created.setCreatedBy(me.getId());
                    created = conversationRepository.save(created);

                    Participant p1 = new Participant();
                    p1.setConversationId(created.getConversationId());
                    p1.setUserId(me.getId());
                    p1.setRole(Participant.GetRole.MEMBER);
                    p1.setAddedAt(Instant.now());

                    Participant p2 = new Participant();
                    p2.setConversationId(created.getConversationId());
                    p2.setUserId(otherId);
                    p2.setRole(Participant.GetRole.MEMBER);
                    p2.setAddedAt(Instant.now());

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

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Participant me = participantRepository.findByConversationIdAndUserId(conversationId, user.getId()).orElse(null);

        if (me == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User does not exist in group.");
        }

        Instant historyEnd = me.getRemovedAt();

        // if (!participantRepository.existsByConversationIdAndUserId(conversationId,
        // me.getUserId())) {
        // return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not a participant");
        // }

        if (historyEnd == null) {
            historyEnd = Instant.now();
        }

        List<Message> messages = messageRepository.findByConversationIdAndCreatedAtBetweenOrderByIdAsc(conversationId,
                me.getAddedAt(), historyEnd);

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
        created.setType(Conversation.GroupType.GROUP);
        created.setName(request.getName());
        created.setCreatedBy(me.getId());
        created.setDescription(request.getDescription());
        created = conversationRepository.save(created);

        Participant p1 = new Participant();
        p1.setConversationId(created.getConversationId());
        p1.setUserId(me.getId());
        p1.setRole(Participant.GetRole.ADMIN);
        p1.setAddedAt(Instant.now());
        p1.setRemovedAt(null);
        participantRepository.save(p1);

        for (Long userId : request.getParticipantIds()) {
            Participant p = new Participant();
            p.setConversationId(created.getConversationId());
            p.setUserId(userId);
            p.setRole(Participant.GetRole.MEMBER);
            p.setAddedAt(Instant.now());
            p.setRemovedAt(null);
            participantRepository.save(p);
        }

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", created.getConversationId());
        dto.put("type", created.getType());
        dto.put("name", created.getName());
        dto.put("description", created.getDescription());
        dto.put("createAt", created.getCreatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/{conversationId}/participants")
    @Transactional
    public ResponseEntity<?> addParticipants(@PathVariable Long conversationId,
            @RequestBody AlterParticipantRequest request, Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Participant me = participantRepository.findByConversationIdAndUserId(conversationId, user.getId()).orElse(null);

        if (me == null || me.getRemovedAt() != null || !"ADMIN".equals(me.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not an admin");
        }

        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null || !"GROUP".equals(conversation.getType())) {
            return ResponseEntity.badRequest().body("Not a group");
        }

        if (request.getParticipantUserIds() == null || request.getParticipantUserIds().isEmpty()) {
            return ResponseEntity.badRequest().body("No users given");
        }

        Set<Long> ids = new HashSet<>(request.getParticipantUserIds());
        if (userRepository.findAllById(ids).size() != ids.size()) {
            return ResponseEntity.badRequest().body("Unknown user");
        }

        for (Long userId : request.getParticipantUserIds()) {
            Participant p = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                    .orElseGet(Participant::new);

            if (p.getId() != null && p.getRemovedAt() == null)
                continue;

            p.setConversationId(conversationId);
            p.setUserId(userId);
            p.setAddedAt(Instant.now());
            p.setRole(Participant.GetRole.MEMBER);
            p.setRemovedAt(null);
            participantRepository.save(p);
        }

        return ResponseEntity.ok("Participants Added.");
    }

    @DeleteMapping("/{conversationId}/participants")
    @Transactional
    public ResponseEntity<?> deleteParticipants(@PathVariable Long conversationId,
            @RequestBody AlterParticipantRequest request, Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Participant me = participantRepository.findByConversationIdAndUserId(conversationId, user.getId()).orElse(null);

        if (me == null || me.getRemovedAt() != null || !"ADMIN".equals(me.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not an admin");
        }

        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null || !"GROUP".equals(conversation.getType())) {
            return ResponseEntity.badRequest().body("Not a group");
        }

        if (request.getParticipantUserIds() == null || request.getParticipantUserIds().isEmpty()) {
            return ResponseEntity.badRequest().body("No users given");
        }

        Set<Long> ids = new HashSet<>(request.getParticipantUserIds());
        if (userRepository.findAllById(ids).size() != ids.size()) {
            return ResponseEntity.badRequest().body("Unknown user");
        }

        for (Long userId : request.getParticipantUserIds()) {
            Participant p = participantRepository.findByConversationIdAndUserId(conversationId, userId).orElseThrow();

            if (p == null || p.getRemovedAt() != null)
                continue;

            p.setRemovedAt(Instant.now());
            participantRepository.save(p);
        }

        return ResponseEntity.ok("Participants deleted.");
    }

    @GetMapping("/{conversationId}/members")
    public ResponseEntity<?> viewMembers(@PathVariable Long conversationId, Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        if (!participantRepository.existsByConversationIdAndUserIdAndRemovedAtIsNull(conversationId, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not an active member.");
        }

        List<Participant> participants = participantRepository.findByConversationIdAndRemovedAtIsNull(conversationId);

        Map<Long, User> users = userRepository
                .findAllById(participants.stream().map(Participant::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> result = participants.stream().map(p -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", p.getUserId());
            dto.put("username", users.get(p.getUserId()).getUsername());
            dto.put("role", p.getRole());
            return dto;
        }).toList();

        return ResponseEntity.ok(result);
    }

    // mark as read
    @PostMapping("/{conversationId}/read")
    @Transactional
    public ResponseEntity<?> markAsRead(@PathVariable Long conversationId, Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Participant me = participantRepository.findByConversationIdAndUserId(conversationId, user.getId())
                .orElseThrow();

        if (me == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Conversation not found");
        }

        Long latest = messageRepository.findTopByConversationIdOrderByIdDesc(conversationId).map(Message::getId)
                .orElse(0L);

        if (me.getLastReadMessageId() == null || latest > me.getLastReadMessageId()) {
            me.setLastReadMessageId(latest);
            participantRepository.save(me);
        }

        return ResponseEntity.ok().build();
    }
}
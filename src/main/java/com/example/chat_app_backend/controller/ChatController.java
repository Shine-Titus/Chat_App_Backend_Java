package com.example.chat_app_backend.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.chat_app_backend.models.ChatMessage;
import com.example.chat_app_backend.models.Message;
import com.example.chat_app_backend.repository.MessageRepository;
import com.example.chat_app_backend.repository.UserRepository;
import com.example.chat_app_backend.repository.ParticipantRepository;
import com.example.chat_app_backend.models.User;
import com.example.chat_app_backend.models.Participant;

import java.security.Principal;
import java.util.*;

@Controller
public class ChatController {

    private final MessageRepository messageRepository;

    private final ParticipantRepository participantRepository;

    private final UserRepository userRepository;

    private final SimpMessagingTemplate messageTemplate;

    public ChatController(MessageRepository messageRepository, UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate, ParticipantRepository participantRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messageTemplate = messagingTemplate;
        this.participantRepository = participantRepository;
    }

    // broadcsting
    @MessageMapping("/chat")
    public void sendMessage(@Payload ChatMessage chatMessage, Principal principal) {

        User me = userRepository.findByUsername(principal.getName()).orElseThrow();
        Long conversationId = chatMessage.getConversationId();
        String content = chatMessage.getContent();

        if (conversationId == null || content == null || content.isBlank()) {
            return;
        }

        if (!participantRepository.existsByConversationIdAndUserIdAndRemovedAtIsNull(conversationId, me.getId())) {
            return;
        }

        Message message = new Message();
        message.setSenderId(me.getId());
        message.setConversationId(conversationId);
        message.setContent(content);
        Message savedMessage = messageRepository.save(message);

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", savedMessage.getId());
        dto.put("senderId", savedMessage.getSenderId());
        dto.put("conversationId", savedMessage.getConversationId());
        dto.put("senderUsername", me.getUsername());
        dto.put("content", savedMessage.getContent());
        dto.put("createdAt", savedMessage.getCreatedAt());

        List<Long> ids = participantRepository.findByConversationIdAndRemovedAtIsNull(conversationId).stream()
                .map(Participant::getUserId)
                .toList();
        for (User u : userRepository.findAllById(ids)) {
            messageTemplate.convertAndSendToUser(u.getUsername(), "/queue/messages", dto);
        }

    }
}
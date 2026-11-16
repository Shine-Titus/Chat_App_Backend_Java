package com.example.chat_app_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.messaging.handler.annotation.Payload;
// import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;

import com.example.chat_app_backend.repository.UserRepository;
import com.example.chat_app_backend.models.User;
import com.example.chat_app_backend.models.ScheduledMessage;

import com.example.chat_app_backend.repository.ScheduledMessageRepository;
import com.example.chat_app_backend.schemas.request.ChatMessageRequest;
import com.example.chat_app_backend.schemas.request.ScheduledMessageRequest;
import com.example.chat_app_backend.service.MessageService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.*;

import org.springframework.security.core.Authentication;

@RestController
public class ChatController {

    private final UserRepository userRepository;

    private final ScheduledMessageRepository scheduledMessageRepository;

    private final MessageService messageService;

    public ChatController( UserRepository userRepository, 
        ScheduledMessageRepository scheduledMessageRepository, 
        MessageService messageService
    ) {
        this.userRepository = userRepository;
        this.scheduledMessageRepository = scheduledMessageRepository;
        this.messageService = messageService;
    }

    // broadcsting
    @MessageMapping("/chat")
    public void broadcastMessage(@Payload ChatMessageRequest chatMessage, Authentication authentication) {
        messageService.sendMessage(chatMessage, authentication);
    }
    
    @PostMapping("/schedule-message")
    public ResponseEntity<ScheduledMessage> scheduleMessage(@RequestBody ScheduledMessageRequest request, Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        Instant sendAt = Instant.ofEpochMilli(request.getSendAt())
        .truncatedTo(ChronoUnit.MINUTES);

        if (sendAt.isBefore(Instant.now().truncatedTo(ChronoUnit.MINUTES))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sendAt must be in the future");
        }

        ScheduledMessage.Status status = ScheduledMessage.Status.PENDING;

        ScheduledMessage scheduledMessage = new ScheduledMessage();

        scheduledMessage.setConversationId(request.getConversationId());    
        scheduledMessage.setSenderId(user.getId());
        scheduledMessage.setContent(request.getContent());
        scheduledMessage.setSendAt(sendAt);
        scheduledMessage.setStatus(status);

        ScheduledMessage savedScheduledMessage = scheduledMessageRepository.save(scheduledMessage);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedScheduledMessage);

    }

    @GetMapping("/schedule-message/{conversationId}")
    public List<ScheduledMessage> getPendingMessages(@PathVariable Long conversationId, Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        List<ScheduledMessage> pending = scheduledMessageRepository.findByConversationIdAndSenderIdAndStatus(conversationId, user.getId(), ScheduledMessage.Status.PENDING);

        return pending;
    }

    @DeleteMapping("/schedule-message/{id}")
    public ResponseEntity<?> deletePendingMessage(@PathVariable Long id, Authentication authentication) {
        // User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        scheduledMessageRepository.deleteById(id);
        return ResponseEntity.ok("scheduled message deleted.");
    }
}
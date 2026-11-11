package com.example.chat_app_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;

import com.example.chat_app_backend.repository.MessageRepository;
import com.example.chat_app_backend.repository.UserRepository;
import com.example.chat_app_backend.schemas.ChatMessageRequest;
import com.example.chat_app_backend.repository.ParticipantRepository;
import com.example.chat_app_backend.models.User;
import com.example.chat_app_backend.models.ScheduledMessage;

import com.example.chat_app_backend.repository.ScheduledMessageRepository;
import com.example.chat_app_backend.schemas.ScheduledMessageRequest;
import com.example.chat_app_backend.service.MessageService;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.core.Authentication;

@Controller
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
    public void sendMessage(@Payload ChatMessageRequest chatMessage, Principal principal) {
        User me = userRepository.findByUsername(principal.getName()).orElseThrow();
        messageService.sendMessage(me.getId(), chatMessage.getConversationId(), chatMessage.getContent());
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
}
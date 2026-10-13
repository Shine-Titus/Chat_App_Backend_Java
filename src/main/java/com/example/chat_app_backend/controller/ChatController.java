package com.example.chat_app_backend.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.chat_app_backend.models.ChatMessage;
import com.example.chat_app_backend.models.Message;
import com.example.chat_app_backend.repository.MessageRepository;
import com.example.chat_app_backend.repository.UserRepository;
import com.example.chat_app_backend.models.User;

import java.security.Principal;

@Controller
public class ChatController {

    private final MessageRepository messageRepository;

    private final UserRepository userRepository;

    private final SimpMessagingTemplate messageTemplate;

    public ChatController(MessageRepository messageRepository, UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messageTemplate = messagingTemplate;
    }

    // broadcsting
    @MessageMapping("/chat")
    public void sendMessage(@Payload ChatMessage chatMessage, Principal principal) {

        User me = userRepository.findByUsername(principal.getName()).orElseThrow();
        User receiver = userRepository.findById(chatMessage.getReceiverId()).orElseThrow();

        Message message = new Message(me.getId(), chatMessage.getReceiverId(), chatMessage.getContent());

        Message savedMessage = messageRepository.save(message);

        chatMessage.setId(savedMessage.getId());
        chatMessage.setSenderId(me.getId());
        chatMessage.setCreatedAt(savedMessage.getCreatedAt().toString());

        messageTemplate.convertAndSendToUser(me.getUsername(), "/queue/messages", chatMessage);
        messageTemplate.convertAndSendToUser(receiver.getUsername(), "/queue/messages", chatMessage);

    };
};
package com.example.chat_app_backend.controller;

import java.util.*;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.security.core.Authentication;

import com.example.chat_app_backend.models.Message;
import com.example.chat_app_backend.repository.MessageRepository;
import com.example.chat_app_backend.models.User;
import com.example.chat_app_backend.repository.UserRepository;

@RestController
@RequestMapping("/messages")
@CrossOrigin(origins = "http://localhost:5173")
public class MessageController {

    private final UserRepository userRepository;

    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository,
            UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    // @GetMapping
    // public List<Message> getMessages() {
    // return messageRepository.findAllByOrderByCreatedAtAsc();
    // }

    @GetMapping("/{otherId}")
    public List<Message> getMessages(@PathVariable Long otherId, Authentication authentication) {

        String myUsername = authentication.getName();

        User me = userRepository.findByUsername(myUsername).orElseThrow();

        return messageRepository.findConversation(me.getId(), otherId);

    }
}
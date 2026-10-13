package com.example.chat_app_backend.controller;

import java.util.*;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.chat_app_backend.models.User;
import com.example.chat_app_backend.repository.UserRepository;
import com.example.chat_app_backend.service.PresenceListener;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final PresenceListener presenceListener;

    public UserController(UserRepository userRepository, PresenceListener presenceListener) {
        this.userRepository = userRepository;
        this.presenceListener = presenceListener;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers(Authentication authentication) {
        String myUsername = authentication.getName();

        User user = userRepository.findByUsername(myUsername).orElseThrow();

        return userRepository.findByIdNot(user.getId()).stream()
                .map(u -> Map.<String, Object>of("id", u.getId(), "username", u.getUsername(), "mobile", u.getMobile()))
                .toList();
    }

    @GetMapping("/online")
    public Set<String> getOnlineUsers() {
        return presenceListener.getOnlineUsernames();
    }

}
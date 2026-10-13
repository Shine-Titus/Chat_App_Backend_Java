package com.example.chat_app_backend.service;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class PresenceListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, Set<String>> online = new ConcurrentHashMap<>();

    public PresenceListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        Principal user = event.getUser();
        if (user == null) {
            return;
        }

        String username = user.getName();
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();

        AtomicBoolean cameOnline = new AtomicBoolean(false);

        online.compute(username, (key, sessions) -> {
            if (sessions == null) {
                sessions = new HashSet<>();
                cameOnline.set(true);
            }
            sessions.add(sessionId);
            return sessions;
        });

        if (cameOnline.get()) {
            broadcast(username, true);
        }
        ;

    };

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user == null) {
            return;
        }

        String username = user.getName();
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();

        AtomicBoolean wentOffline = new AtomicBoolean(false);

        online.compute(username, (key, sessions) -> {
            if (sessions == null)
                return null;
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                wentOffline.set(true);
                return null;
            }
            return sessions;
        });

        if (wentOffline.get()) {
            broadcast(username, false);
        }
        ;

    }

    public Set<String> getOnlineUsernames() {
        return Set.copyOf(online.keySet());
    }

    private void broadcast(String username, boolean isOnline) {
        Map<String, Object> payload = Map.<String, Object>of(
                "username", username,
                "online", isOnline);

        messagingTemplate.convertAndSend("/topic/presence", (Object) payload);
    }
}

package com.example.chat_app_backend.config;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Component
public class SessionCheckInterceptor implements ChannelInterceptor {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public SessionCheckInterceptor(FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.SEND.equals(accessor.getCommand())) {
            Map<String, Object> attrs = accessor.getSessionAttributes();
            String sessionId = attrs == null ? null
                    : (String) attrs.get(HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME);

            if (sessionId == null || sessionRepository.findById(sessionId) == null) {
                throw new MessageDeliveryException("Session ended");
            }
        }
        return message;
    }
}

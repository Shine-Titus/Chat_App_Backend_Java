package com.example.chat_app_backend.service;

import org.springframework.stereotype.Service;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.chat_app_backend.repository.MessageRepository;
import com.example.chat_app_backend.repository.ParticipantRepository;
import com.example.chat_app_backend.repository.UserRepository;

import com.example.chat_app_backend.models.User;
import com.example.chat_app_backend.models.Message;
import com.example.chat_app_backend.models.Participant;
import java.util.*;


@Service
public class MessageService {

    private final MessageRepository messageRepository;

    private final ParticipantRepository participantRepository;

    private final UserRepository userRepository;

    private final SimpMessagingTemplate messageTemplate;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository,
            SimpMessagingTemplate messagingTemplate, ParticipantRepository participantRepository
        ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messageTemplate = messagingTemplate;
        this.participantRepository = participantRepository;
    }

    
    public boolean sendMessage(Long senderId, Long conversationId, String content) {

        if (conversationId == null || content == null || content.isBlank()) {
            return false;
        }

        User sender = userRepository.findById(senderId).orElseThrow();

        if (!participantRepository.existsByConversationIdAndUserIdAndRemovedAtIsNull(conversationId, senderId)) {
            return false;
        }

        Message message = new Message();
        message.setSenderId(senderId);
        message.setConversationId(conversationId);
        message.setContent(content);
        Message savedMessage = messageRepository.save(message);

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", savedMessage.getId());
        dto.put("senderId", savedMessage.getSenderId());
        dto.put("conversationId", savedMessage.getConversationId());
        dto.put("senderUsername", sender.getUsername());
        dto.put("content", savedMessage.getContent());
        dto.put("createdAt", savedMessage.getCreatedAt());

        List<Long> ids = participantRepository.findByConversationIdAndRemovedAtIsNull(conversationId).stream()
                .map(Participant::getUserId)
                .toList();
        for (User u : userRepository.findAllById(ids)) {
            messageTemplate.convertAndSendToUser(u.getUsername(), "/queue/messages", dto);
        }

        return true;
    }

}

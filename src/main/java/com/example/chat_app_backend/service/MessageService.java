package com.example.chat_app_backend.service;

import org.springframework.stereotype.Service;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.chat_app_backend.repository.MessageRepository;
import com.example.chat_app_backend.repository.ParticipantRepository;
import com.example.chat_app_backend.repository.UserRepository;
import com.example.chat_app_backend.repository.FileAttachmentRepository;

import com.example.chat_app_backend.schemas.request.ChatMessageRequest;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.security.core.Authentication;

import com.example.chat_app_backend.models.User;
import com.example.chat_app_backend.models.FileAttachment;
import com.example.chat_app_backend.models.Message;

import com.example.chat_app_backend.dto.MemberView;

import java.util.*;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    private final ParticipantRepository participantRepository;

    private final SimpMessagingTemplate messageTemplate;

    private final UserRepository userRepository;

    private final FileAttachmentRepository fileAttachmentRepository;

    public MessageService(MessageRepository messageRepository, 
        UserRepository userRepository,
        SimpMessagingTemplate messagingTemplate, 
        ParticipantRepository participantRepository,
        FileAttachmentRepository fileAttachmentRepository
        ) {

        this.messageRepository = messageRepository;
        this.messageTemplate = messagingTemplate;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.fileAttachmentRepository = fileAttachmentRepository;
    }

    
    public boolean sendMessage(@RequestBody ChatMessageRequest chatMessage, Authentication authentication) {

        Long conversationId = chatMessage.getConversationId();
        Long senderId = chatMessage.getSenderId();
        String content = chatMessage.getContent();
        Message.Type type = chatMessage.getType();
        Long fileAttachmentId = chatMessage.getFileAttachmentId();

        if (authentication != null) {
            User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
            if (senderId == null || !senderId.equals(user.getId())) return false;
        }
        
        if (conversationId == null) { return false;}
        if (type == Message.Type.FILE && fileAttachmentId == null) { return false; }
        if (content == null || content.trim().isEmpty()) { content = ""; }
        if (type == null) { type = Message.Type.TEXT; }
        if (type == Message.Type.TEXT && content.isEmpty()) { return false; }

        List<MemberView> members = participantRepository.findActiveMembers(conversationId);

         MemberView sender = members.stream()
            .filter(m -> m.getUserId().equals(senderId))
            .findFirst().orElse(null);

        if (sender == null) return false;   // not an active member

        // File validation
        FileAttachment fa = null;
        if (type == Message.Type.FILE) {
            if (fileAttachmentId == null) return false;

            fa = fileAttachmentRepository.findById(fileAttachmentId).orElse(null);
            if (fa == null) return false;

            // must be uploaded by this sender, into this same conversation
            if (!fa.getUploaderId().equals(senderId)
                    || !fa.getConversationId().equals(conversationId)) return false;

            // optional: stop the same upload being sent twice
            // if (messageRepository.existsByFileAttachmentId(fileAttachmentId)) return false;
        }

        Message message = new Message();
        message.setSenderId(senderId);
        message.setConversationId(conversationId);
        message.setContent(content);
        message.setType(type);                                       
        message.setFileAttachmentId(fa != null ? fa.getId() : null);
        Message savedMessage = messageRepository.save(message);

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", savedMessage.getId());
        dto.put("senderId", savedMessage.getSenderId());
        dto.put("conversationId", savedMessage.getConversationId());
        dto.put("senderUsername", sender.getUsername());
        dto.put("content", savedMessage.getContent());
        dto.put("createdAt", savedMessage.getCreatedAt());
        dto.put("type", type.name());

        if (type == Message.Type.FILE) {
            dto.put("type", "FILE");
            dto.put("fileAttachmentId", fileAttachmentId);
        }
        
        for (MemberView m : members) {
            messageTemplate.convertAndSendToUser(m.getUsername(), "/queue/messages", dto);
        }
        return true;
    }
}

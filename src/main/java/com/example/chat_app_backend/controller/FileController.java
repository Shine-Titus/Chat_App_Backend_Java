package com.example.chat_app_backend.controller;

import java.io.IOException;

import com.example.chat_app_backend.service.MessageService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

import com.example.chat_app_backend.service.FileStorageService;
import com.example.chat_app_backend.models.FileAttachment;
import com.example.chat_app_backend.models.Message;
import com.example.chat_app_backend.repository.FileAttachmentRepository;
import com.example.chat_app_backend.repository.ParticipantRepository;

import com.example.chat_app_backend.dto.FileResponse;

import com.example.chat_app_backend.models.User;
import com.example.chat_app_backend.repository.UserRepository;
import com.example.chat_app_backend.schemas.request.ChatMessageRequest;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileStorageService storage;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    public FileController(FileStorageService storage,
                          FileAttachmentRepository fileAttachmentRepository,
                          ParticipantRepository participantRepository,
                          UserRepository userRepository,
                          MessageService messageService) {

        this.storage = storage;
        this.fileAttachmentRepository = fileAttachmentRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<?> upload(@RequestParam MultipartFile file,
                               @RequestParam Long conversationId,
                               @RequestParam(required = false) String caption,
                               Authentication authentication) throws IOException {
        
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        if (!participantRepository.existsByConversationIdAndUserId(conversationId, user.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        FileAttachment fa = storage.store(file, user.getId(), conversationId);

        ChatMessageRequest req = new ChatMessageRequest();
        req.setSenderId(user.getId());
        req.setConversationId(conversationId);
        req.setContent(caption);
        req.setType(Message.Type.FILE);
        req.setFileAttachmentId(fa.getId());

        boolean ok = messageService.sendMessage(req, authentication);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not send file message");
        }

        return ResponseEntity.ok(FileResponse.from(fa));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id,
                                             Authentication authentication) throws IOException {
                                                
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        FileAttachment fa = fileAttachmentRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!participantRepository.existsByConversationIdAndUserId(fa.getConversationId(), user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fa.getOriginalName()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(storage.load(fa));
    }

}

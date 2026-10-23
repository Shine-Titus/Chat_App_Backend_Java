package com.example.chat_app_backend.controller;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.example.chat_app_backend.models.*;

import com.example.chat_app_backend.schemas.CreateTagRequest;
import com.example.chat_app_backend.schemas.TagConversationsRequest;

import org.springframework.transaction.annotation.Transactional;

import com.example.chat_app_backend.repository.*;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/tags")
public class TagController {

    private static final int MAX_TAGS = 20;
    private static final int MAX_NAME = 30;

    private final TagRepository tagRepository;
    private final TagConversationRepository tagConversationRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;

    public TagController(TagRepository tagRepository,
            TagConversationRepository tagConversationRepository,
            UserRepository userRepository,
            ParticipantRepository participantRepository) {
        this.tagRepository = tagRepository;
        this.tagConversationRepository = tagConversationRepository;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
    }

    private Map<String, Object> toDto(Tag tag, List<Long> conversationIds) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", tag.getId());
        dto.put("name", tag.getName());
        dto.put("color", tag.getColor());
        dto.put("conversationIds", conversationIds);
        return dto;
    }

    private List<Long> idsOf(Long tagId) {
        return tagConversationRepository.findByTagId(tagId).stream()
                .map(TagConversation::getConversationId).toList();
    }

    @GetMapping
    public ResponseEntity<?> getTags(Authentication authentication) {
        User me = userRepository.findByUsername(authentication.getName()).orElseThrow();
        List<Tag> tags = tagRepository.findByOwnerId(me.getId());

        // one query for all links, grouped by tag
        Map<Long, List<Long>> byTag = tagConversationRepository
                .findByTagIdIn(tags.stream().map(Tag::getId).toList()).stream()
                .collect(Collectors.groupingBy(TagConversation::getTagId,
                        Collectors.mapping(TagConversation::getConversationId, Collectors.toList())));

        return ResponseEntity.ok(tags.stream()
                .map(t -> toDto(t, byTag.getOrDefault(t.getId(), List.of())))
                .toList());
    }

    @PostMapping
    public ResponseEntity<?> createTag(@RequestBody CreateTagRequest request,
            Authentication authentication) {
        User me = userRepository.findByUsername(authentication.getName()).orElseThrow();

        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty() || name.length() > MAX_NAME) {
            return ResponseEntity.badRequest().body("Name must be 1-" + MAX_NAME + " characters");
        }
        if (tagRepository.countByOwnerId(me.getId()) >= MAX_TAGS) {
            return ResponseEntity.badRequest().body("Tag limit reached");
        }
        if (tagRepository.existsByOwnerIdAndNameIgnoreCase(me.getId(), name)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Tag already exists");
        }

        Tag tag = new Tag();
        tag.setOwnerId(me.getId());
        tag.setName(name);
        tag.setColor(request.getColor());
        tag = tagRepository.save(tag);

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(tag, List.of()));
    }

    @PutMapping("/{tagId}")
    public ResponseEntity<?> updateTag(@PathVariable Long tagId,
            @RequestBody CreateTagRequest request,
            Authentication authentication) {
        User me = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Tag tag = tagRepository.findByIdAndOwnerId(tagId, me.getId()).orElse(null);
        if (tag == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tag not found");

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty() || name.length() > MAX_NAME) {
                return ResponseEntity.badRequest().body("Name must be 1-" + MAX_NAME + " characters");
            }
            if (!name.equalsIgnoreCase(tag.getName())
                    && tagRepository.existsByOwnerIdAndNameIgnoreCase(me.getId(), name)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Tag already exists");
            }
            tag.setName(name);
        }
        if (request.getColor() != null)
            tag.setColor(request.getColor());

        tag = tagRepository.save(tag);
        return ResponseEntity.ok(toDto(tag, idsOf(tag.getId())));
    }

    @DeleteMapping("/{tagId}")
    @Transactional
    public ResponseEntity<?> deleteTag(@PathVariable Long tagId, Authentication authentication) {
        User me = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Tag tag = tagRepository.findByIdAndOwnerId(tagId, me.getId()).orElse(null);
        if (tag == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tag not found");

        tagConversationRepository.deleteByTagId(tagId); // delete links first
        tagRepository.delete(tag);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{tagId}/conversations")
    @Transactional
    public ResponseEntity<?> addConversations(@PathVariable Long tagId,
            @RequestBody TagConversationsRequest request,
            Authentication authentication) {
        User me = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Tag tag = tagRepository.findByIdAndOwnerId(tagId, me.getId()).orElse(null);
        if (tag == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tag not found");

        if (request.getConversationIds() == null || request.getConversationIds().isEmpty()) {
            return ResponseEntity.badRequest().body("No conversations given");
        }

        Set<Long> ids = new HashSet<>(request.getConversationIds());

        // I must belong to every conversation I tag
        if (participantRepository.findByUserIdAndConversationIdIn(me.getId(), ids).size() != ids.size()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Conversation not found");
        }

        for (Long cid : ids) {
            if (tagConversationRepository.existsByTagIdAndConversationId(tagId, cid))
                continue;
            TagConversation link = new TagConversation();
            link.setTagId(tagId);
            link.setConversationId(cid);
            tagConversationRepository.save(link);
        }

        return ResponseEntity.ok(toDto(tag, idsOf(tagId)));
    }

    @DeleteMapping("/{tagId}/conversations/{conversationId}")
    @Transactional
    public ResponseEntity<?> removeConversation(@PathVariable Long tagId,
            @PathVariable Long conversationId,
            Authentication authentication) {
        User me = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Tag tag = tagRepository.findByIdAndOwnerId(tagId, me.getId()).orElse(null);
        if (tag == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tag not found");

        tagConversationRepository.deleteByTagIdAndConversationId(tagId, conversationId);
        return ResponseEntity.ok(toDto(tag, idsOf(tagId)));
    }
}
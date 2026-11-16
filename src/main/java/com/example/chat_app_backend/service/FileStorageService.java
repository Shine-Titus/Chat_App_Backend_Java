package com.example.chat_app_backend.service;

import com.example.chat_app_backend.models.FileAttachment;
import com.example.chat_app_backend.repository.FileAttachmentRepository;  

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;  
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;


@Service
public class FileStorageService {
    private final Path root;
    private final FileAttachmentRepository repo;

    public FileStorageService(@Value("${app.upload.dir}") String dir,
                              FileAttachmentRepository repo) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        this.repo = repo;
    }

    public FileAttachment store(MultipartFile file, Long userId, Long conversationId) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("Empty file");

        String original = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (original.contains("..")) throw new IllegalArgumentException("Invalid filename");

        String storedName = UUID.randomUUID().toString();
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) throw new SecurityException("Bad path");

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        FileAttachment fa = new FileAttachment();
        fa.setOriginalName(original);
        fa.setStoredName(storedName);
        fa.setContentType(file.getContentType());
        fa.setSize(file.getSize());
        fa.setUploaderId(userId);
        fa.setConversationId(conversationId);
        return repo.save(fa);
    }

    public Resource load(FileAttachment fa) throws MalformedURLException {
        return new UrlResource(root.resolve(fa.getStoredName()).toUri());
    }
}

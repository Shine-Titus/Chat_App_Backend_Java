package com.example.chat_app_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.chat_app_backend.models.FileAttachment;
import java.util.Optional;
import java.util.List;

public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {
        Optional<FileAttachment> findById(Long id);
        List<FileAttachment> findAllById(Iterable<Long> ids);
        
}

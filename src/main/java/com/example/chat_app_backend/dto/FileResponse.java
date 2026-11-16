package com.example.chat_app_backend.dto;
import com.example.chat_app_backend.models.FileAttachment;
import java.time.Instant;

public record FileResponse(Long id,
                           String fileName,
                           String contentType,
                           long size,
                           Instant uploadedAt) {

    public static FileResponse from(FileAttachment fa) {
        return new FileResponse(
                fa.getId(),
                fa.getOriginalName(),
                fa.getContentType(),
                fa.getSize(),
                fa.getUploadedAt());
    }
}
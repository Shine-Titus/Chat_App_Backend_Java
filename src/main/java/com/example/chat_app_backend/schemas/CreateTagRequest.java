package com.example.chat_app_backend.schemas;

import lombok.Data;

@Data
public class CreateTagRequest {
    private String name;
    private String color;
}
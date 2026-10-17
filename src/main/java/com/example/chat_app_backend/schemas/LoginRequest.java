package com.example.chat_app_backend.models;

import lombok.Data;

@Data
public class LoginRequest {

    private String username;
    private String password;

}

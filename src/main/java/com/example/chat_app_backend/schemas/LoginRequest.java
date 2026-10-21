package com.example.chat_app_backend.schemas;

import lombok.Data;

@Data
public class LoginRequest {

    private String username;
    private String password;

}

package com.example.chat_app_backend.schemas;

import lombok.Data;

@Data
public class RegisterRequest {

    private String username;
    private String password;
    private String mobile;

}

package com.example.chat_app_backend.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.chat_app_backend.models.User;
import com.example.chat_app_backend.repository.UserRepository;
import com.example.chat_app_backend.schemas.LoginRequest;
import com.example.chat_app_backend.schemas.RegisterRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class LoginController {

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    private final SimpMessagingTemplate messagingTemplate;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public LoginController(AuthenticationManager authenticationManager,
            FindByIndexNameSessionRepository<? extends Session> sessionRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SimpMessagingTemplate messagingTemplate) {

        this.authenticationManager = authenticationManager;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

            HttpSession current = httpRequest.getSession(false);
            String currentId = current != null ? current.getId() : null;

            sessionRepository.findByPrincipalName(authentication.getName())
                    .keySet().stream()
                    .filter(id -> !id.equals(currentId))
                    .forEach(sessionRepository::deleteById);

            messagingTemplate.convertAndSendToUser(
                    authentication.getName(), "/queue/kicked", "logged-in-elsewhere");

            if (current != null) {
                httpRequest.changeSessionId();
            }

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            return ResponseEntity.ok("Login successful");

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid username or password");
        }

    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(Map.of("id", user.getId(), "username", user.getUsername()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> Register(@RequestBody RegisterRequest request) {

        if (request.getUsername() == null || request.getUsername().isBlank() || request.getPassword() == null
                || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Username and password is required");
        }

        if (userRepository.findByUsername(request.getUsername().trim()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already taken");
        }

        if (request.getPassword().length() < 8) {
            return ResponseEntity.badRequest().body("Password must be more than 7 characters.");
        }

        if (request.getMobile().length() != 10) {
            return ResponseEntity.badRequest().body("Mobile number should be exactly 10 digits");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setMobile(request.getMobile());
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body("New user created successfully.");

    };
};

package com.vfdcb.bolao.auth.controller;

import com.vfdcb.bolao.auth.dto.LoginRequest;
import com.vfdcb.bolao.auth.dto.SignupRequest;
import com.vfdcb.bolao.auth.dto.UserResponse;
import com.vfdcb.bolao.auth.model.User;
import com.vfdcb.bolao.auth.service.AuthResult;
import com.vfdcb.bolao.auth.service.AuthService;
import com.vfdcb.bolao.auth.util.CookieHelper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieHelper cookieHelper;
    private final String cookieSecret;
    private final int sessionDurationHours;

    public AuthController(AuthService authService,
                          CookieHelper cookieHelper,
                          @Value("${app.security.cookie-hash-key}") String cookieSecret,
                          @Value("${app.security.session-duration-hours:720}") int sessionDurationHours) {
        this.authService = authService;
        this.cookieHelper = cookieHelper;
        this.cookieSecret = cookieSecret;
        this.sessionDurationHours = sessionDurationHours;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        AuthResult result = authService.signup(request.name(), request.email(), request.password());
        setSessionCookie(response, result.session().getId());
        return UserResponse.fromEntity(result.user());
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResult result = authService.login(request.email(), request.password());
        setSessionCookie(response, result.session().getId());
        return UserResponse.fromEntity(result.user());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        UUID sessionId = (UUID) request.getAttribute("sessionID");
        if (sessionId != null) {
            authService.logout(sessionId);
        }

        Cookie cookie = new Cookie("session", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(0);
        // Since Servlet API doesn't easily support SameSite directly on Cookie object in standard ways without setting headers,
        // we'll add SameSite manually if needed in a filter, or just use Spring's ResponseCookie.
        // For simplicity, let's use ResponseCookie.
        response.addHeader("Set-Cookie", "session=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Strict");
    }

    @GetMapping("/me")
    public UserResponse me(HttpServletRequest request) {
        User user = (User) request.getAttribute("user");
        if (user == null) {
            throw new com.vfdcb.bolao.auth.exception.AuthException("Unauthorized");
        }
        return UserResponse.fromEntity(user);
    }

    private void setSessionCookie(HttpServletResponse response, UUID sessionId) {
        String signedValue = cookieHelper.sign(sessionId.toString(), cookieSecret);
        int maxAgeSeconds = sessionDurationHours * 3600;
        
        // Use Set-Cookie header to support SameSite=Strict
        String cookieHeader = String.format("session=%s; Path=/; Max-Age=%d; HttpOnly; Secure; SameSite=Strict",
                signedValue, maxAgeSeconds);
        response.addHeader("Set-Cookie", cookieHeader);
    }
}

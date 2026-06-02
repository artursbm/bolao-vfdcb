package com.vfdcb.bolao.config;

import com.vfdcb.bolao.auth.model.User;
import com.vfdcb.bolao.auth.service.AuthService;
import com.vfdcb.bolao.auth.util.CookieHelper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final CookieHelper cookieHelper;
    private final String cookieSecret;

    public AuthInterceptor(AuthService authService,
                           CookieHelper cookieHelper,
                           @Value("${app.security.cookie-hash-key}") String cookieSecret) {
        this.authService = authService;
        this.cookieHelper = cookieHelper;
        this.cookieSecret = cookieSecret;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String sessionCookieValue = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("session".equals(cookie.getName())) {
                    sessionCookieValue = cookie.getValue();
                    break;
                }
            }
        }

        if (sessionCookieValue == null || sessionCookieValue.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized\"}");
            return false;
        }

        try {
            String sessionIdStr = cookieHelper.verify(sessionCookieValue, cookieSecret);
            UUID sessionId = UUID.fromString(sessionIdStr);

            User user = authService.getCurrentUser(sessionId);

            request.setAttribute("sessionID", sessionId);
            request.setAttribute("user", user);

            return true;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized\"}");
            return false;
        }
    }
}

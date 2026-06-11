package com.vfdcb.bolao.config;

import com.vfdcb.bolao.auth.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final String adminEmail;

    public AdminInterceptor(@Value("${app.admin.email}") String adminEmail) {
        this.adminEmail = adminEmail;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        User user = (User) request.getAttribute("user");
        
        if (user == null || !adminEmail.equals(user.getEmail())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Forbidden: Admin access required\"}");
            return false;
        }

        return true;
    }
}

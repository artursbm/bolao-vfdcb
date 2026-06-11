package com.vfdcb.bolao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final LogInterceptor logInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, AdminInterceptor adminInterceptor, LogInterceptor logInterceptor) {
        this.authInterceptor = authInterceptor;
        this.adminInterceptor = adminInterceptor;
        this.logInterceptor = logInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/auth/signup", "/api/auth/login", "/api/matches", "/api/stream/matches");

        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**");

        registry.addInterceptor(logInterceptor);
    }
}

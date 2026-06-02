package com.vfdcb.bolao.auth.dto;

import com.vfdcb.bolao.auth.model.User;

import java.util.UUID;

public record UserResponse(UUID id, String name, String email) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}

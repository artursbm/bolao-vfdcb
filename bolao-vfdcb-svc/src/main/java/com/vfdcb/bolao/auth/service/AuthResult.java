package com.vfdcb.bolao.auth.service;

import com.vfdcb.bolao.auth.model.Session;
import com.vfdcb.bolao.auth.model.User;

public record AuthResult(User user, Session session) {
}

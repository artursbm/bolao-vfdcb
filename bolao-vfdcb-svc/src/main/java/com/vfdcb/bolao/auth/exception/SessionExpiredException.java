package com.vfdcb.bolao.auth.exception;

public class SessionExpiredException extends AuthException {
    public SessionExpiredException() {
        super("session expired");
    }
}

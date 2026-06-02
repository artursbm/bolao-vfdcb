package com.vfdcb.bolao.auth.exception;

public class InvalidCredentialsException extends AuthException {
    public InvalidCredentialsException() {
        super("invalid credentials");
    }
}

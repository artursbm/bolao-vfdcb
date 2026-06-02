package com.vfdcb.bolao.auth.exception;

public class EmailAlreadyExistsException extends AuthException {
    public EmailAlreadyExistsException() {
        super("email already exists");
    }
}

package com.vfdcb.bolao.config;

import com.vfdcb.bolao.auth.exception.EmailAlreadyExistsException;
import com.vfdcb.bolao.auth.exception.InvalidCredentialsException;
import com.vfdcb.bolao.auth.exception.SessionExpiredException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return Map.of("error", "Email already exists");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleInvalidCredentials(InvalidCredentialsException ex) {
        return Map.of("error", "Invalid credentials");
    }

    @ExceptionHandler(SessionExpiredException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleSessionExpired(SessionExpiredException ex) {
        return Map.of("error", "Session expired");
    }

    @ExceptionHandler(com.vfdcb.bolao.auth.exception.AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> handleAuthException(com.vfdcb.bolao.auth.exception.AuthException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(com.vfdcb.bolao.championship.exception.MatchNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleMatchNotFound(com.vfdcb.bolao.championship.exception.MatchNotFoundException ex) {
        return Map.of("error", "Match not found");
    }

    @ExceptionHandler(com.vfdcb.bolao.championship.exception.MatchAlreadyStartedException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleMatchAlreadyStarted(com.vfdcb.bolao.championship.exception.MatchAlreadyStartedException ex) {
        return Map.of("error", "Match has already started, guesses are closed");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        StringBuilder errors = new StringBuilder("Validation failed:");
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.append(" ").append(error.getField()).append(" ").append(error.getDefaultMessage()).append(";");
        }
        return Map.of("error", errors.toString());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleGenericException(Exception ex) {
        ex.printStackTrace(); // Minimal logging
        return Map.of("error", "Internal server error");
    }
}

package com.vfdcb.bolao.auth.service;

import com.vfdcb.bolao.auth.exception.EmailAlreadyExistsException;
import com.vfdcb.bolao.auth.exception.InvalidCredentialsException;
import com.vfdcb.bolao.auth.exception.SessionExpiredException;
import com.vfdcb.bolao.auth.model.Session;
import com.vfdcb.bolao.auth.model.User;
import com.vfdcb.bolao.auth.repository.SessionRepository;
import com.vfdcb.bolao.auth.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final int sessionDurationHours;

    public AuthService(UserRepository userRepository,
                       SessionRepository sessionRepository,
                       @Value("${app.security.session-duration-hours:720}") int sessionDurationHours) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.sessionDurationHours = sessionDurationHours;
    }

    @Transactional
    public AuthResult signup(String name, String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException();
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        User user = new User(name, email, hashedPassword);
        user = userRepository.save(user);

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(sessionDurationHours);
        Session session = new Session(user.getId(), expiresAt);
        session = sessionRepository.save(session);

        return new AuthResult(user, session);
    }

    @Transactional
    public AuthResult login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(sessionDurationHours);
        Session session = new Session(user.getId(), expiresAt);
        session = sessionRepository.save(session);

        return new AuthResult(user, session);
    }

    @Transactional
    public void logout(UUID sessionId) {
        sessionRepository.deleteById(sessionId);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(UUID sessionId) {
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new InvalidCredentialsException(); // or a specific session not found error
        }

        Session session = sessionOpt.get();

        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            // Ideally clean up expired session, but readOnly transaction means we can't delete here unless we make it writable.
            // In Go it deleted it. We will make it writable to match.
            throw new SessionExpiredException();
        }

        return userRepository.findById(session.getUserId())
                .orElseThrow(InvalidCredentialsException::new);
    }

    @Transactional
    public void deleteExpiredSession(UUID sessionId) {
        sessionRepository.deleteById(sessionId);
    }
}

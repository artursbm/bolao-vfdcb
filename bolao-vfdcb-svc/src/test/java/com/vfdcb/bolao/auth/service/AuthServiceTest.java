package com.vfdcb.bolao.auth.service;

import com.vfdcb.bolao.auth.exception.EmailAlreadyExistsException;
import com.vfdcb.bolao.auth.exception.InvalidCredentialsException;
import com.vfdcb.bolao.auth.exception.SessionExpiredException;
import com.vfdcb.bolao.auth.model.Session;
import com.vfdcb.bolao.auth.model.User;
import com.vfdcb.bolao.auth.repository.SessionRepository;
import com.vfdcb.bolao.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionRepository sessionRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, sessionRepository, 24); // 24 hours
    }

    @Test
    void testSignup_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            session.setId(UUID.randomUUID());
            return session;
        });

        AuthResult result = authService.signup("Test User", "test@example.com", "password123");

        assertNotNull(result);
        assertEquals("Test User", result.user().getName());
        assertEquals("test@example.com", result.user().getEmail());
        assertTrue(BCrypt.checkpw("password123", result.user().getPassword()));
        assertNotNull(result.session());

        verify(userRepository, times(1)).save(any(User.class));
        verify(sessionRepository, times(1)).save(any(Session.class));
    }

    @Test
    void testSignup_DuplicateEmail() {
        User existingUser = new User("Existing", "test@example.com", "hashed");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        assertThrows(EmailAlreadyExistsException.class, () -> 
            authService.signup("User 2", "test@example.com", "password456")
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        String hashedPassword = BCrypt.hashpw("password123", BCrypt.gensalt());
        User user = new User("Test User", "test@example.com", hashedPassword);
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            session.setId(UUID.randomUUID());
            return session;
        });

        AuthResult result = authService.login("test@example.com", "password123");

        assertNotNull(result);
        assertEquals("test@example.com", result.user().getEmail());
        assertNotNull(result.session());
    }

    @Test
    void testLogin_WrongPassword() {
        String hashedPassword = BCrypt.hashpw("password123", BCrypt.gensalt());
        User user = new User("Test User", "test@example.com", hashedPassword);
        
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> 
            authService.login("test@example.com", "wrongpassword")
        );
    }

    @Test
    void testLogin_NonexistentUser() {
        when(userRepository.findByEmail("noone@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> 
            authService.login("noone@example.com", "password123")
        );
    }

    @Test
    void testGetCurrentUser_Success() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Session session = new Session(userId, LocalDateTime.now().plusHours(1));
        session.setId(sessionId);

        User user = new User("Test User", "test@example.com", "hashed");
        user.setId(userId);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = authService.getCurrentUser(sessionId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void testGetCurrentUser_ExpiredSession() {
        UUID sessionId = UUID.randomUUID();
        Session session = new Session(UUID.randomUUID(), LocalDateTime.now().minusHours(1)); // Expired
        
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThrows(SessionExpiredException.class, () -> 
            authService.getCurrentUser(sessionId)
        );
    }

    @Test
    void testLogout() {
        UUID sessionId = UUID.randomUUID();
        authService.logout(sessionId);
        verify(sessionRepository, times(1)).deleteById(sessionId);
    }
}

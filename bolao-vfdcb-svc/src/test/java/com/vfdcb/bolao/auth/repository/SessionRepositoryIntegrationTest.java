package com.vfdcb.bolao.auth.repository;

import com.vfdcb.bolao.auth.model.Session;
import com.vfdcb.bolao.auth.model.User;
import com.vfdcb.bolao.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
public class SessionRepositoryIntegrationTest {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testIntegrationRepositorySession() {
        // Create user
        User user = new User("Session Test", "session@test.com", "hashedpass");
        user = userRepository.save(user);

        // Create session
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        Session session = new Session(user.getId(), expiresAt);
        session = sessionRepository.save(session);

        assertThat(session.getUserId()).isEqualTo(user.getId());

        // Get session
        Optional<Session> retrievedSession = sessionRepository.findById(session.getId());
        assertThat(retrievedSession).isPresent();
        assertThat(retrievedSession.get().getId()).isEqualTo(session.getId());

        // Delete session
        sessionRepository.deleteById(session.getId());

        // Verify deletion
        Optional<Session> deletedSession = sessionRepository.findById(session.getId());
        assertThat(deletedSession).isEmpty();
    }
}

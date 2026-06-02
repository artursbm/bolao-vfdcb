package com.vfdcb.bolao.auth.repository;

import com.vfdcb.bolao.auth.model.User;
import com.vfdcb.bolao.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
public class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testIntegrationRepositoryCreateUser() {
        User user = new User("Integration Test", "integration@test.com", "hashedpassword");
        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("Integration Test");
        assertThat(savedUser.getEmail()).isEqualTo("integration@test.com");
    }

    @Test
    void testIntegrationRepositoryGetUserByEmail() {
        User user = new User("Test User", "getbyemail@test.com", "hashedpass");
        userRepository.save(user);

        Optional<User> retrievedUser = userRepository.findByEmail("getbyemail@test.com");

        assertThat(retrievedUser).isPresent();
        assertThat(retrievedUser.get().getId()).isEqualTo(user.getId());
    }

    @Test
    void testIntegrationRepositoryGetUserByID() {
        User user = new User("Test User", "getbyid@test.com", "hashedpass");
        userRepository.save(user);

        Optional<User> retrievedUser = userRepository.findById(user.getId());

        assertThat(retrievedUser).isPresent();
        assertThat(retrievedUser.get().getEmail()).isEqualTo("getbyid@test.com");
    }
}

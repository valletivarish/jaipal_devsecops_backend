package com.airquality.config;

import com.airquality.model.User;
import com.airquality.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        createDemoUserIfNotExists("qa@test.com", "qauser", "QA Tester", "SecurePass123");
        createDemoUserIfNotExists("user2@test.com", "demouser2", "Demo User 2", "SecurePass123");
    }

    private void createDemoUserIfNotExists(String email, String username, String fullName, String password) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setEmail(email);
            user.setUsername(username);
            user.setFullName(fullName);
            user.setHashedPassword(passwordEncoder.encode(password));
            user.setIsActive(true);
            userRepository.save(user);
        }
    }
}

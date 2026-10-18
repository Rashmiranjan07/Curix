package com.curix.config;

import com.curix.entity.User;
import com.curix.enums.Role;
import com.curix.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.countByRole(Role.ADMIN) == 0) {
            User admin = User.builder()
                    .email("admin@gmail.com")
                    .password(passwordEncoder.encode("Admin@2026"))
                    .role(Role.ADMIN)
                    .active(true)
                    .verified(true)
                    .build();

            userRepository.save(admin);
            log.info(">>> [Curix Bootstrap] Initial Administrator provisioned: admin@gmail.com / Admin@2026");
        }
    }
}
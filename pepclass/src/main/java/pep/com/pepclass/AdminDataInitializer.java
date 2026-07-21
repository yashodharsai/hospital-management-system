package pep.com.pepclass;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedDemoUser("chairman", "chairman123", "Hospital Chairman", Role.CHAIRMAN);
        seedDemoUser("doctor", "doctor123", "Dr. Alice Morgan", Role.DOCTOR);
        seedDemoUser("patient", "patient123", "Patient Bob Carter", Role.PATIENT);
    }

    private void seedDemoUser(String username, String rawPassword, String fullName, Role role) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setFullName(fullName);
            user.setEmail(username + "@hospital.local");
            user.setPhone("0000000000");
            user.setRole(role);
            user.setEnabled(true);
            userRepository.save(user);
        }
    }
}

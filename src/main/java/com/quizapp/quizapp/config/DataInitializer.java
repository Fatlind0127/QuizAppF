package com.quizapp.quizapp.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.quizapp.quizapp.user.User;
import com.quizapp.quizapp.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.quizapp.quizapp.subject.Subject;
import com.quizapp.quizapp.subject.SubjectRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubjectRepository subjectRepository;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, SubjectRepository subjectRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.subjectRepository = subjectRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create default SupervisorAdmin if not exists
        if (!userRepository.existsByUsername("SupervisorAdmin")) {
            User supervisor = new User();
            supervisor.setUsername("SupervisorAdmin");
            supervisor.setPassword(passwordEncoder.encode("1234"));
            supervisor.setRole(User.Role.SUPERVISOR_ADMIN);
            supervisor.setFullName("Supervisor Administrator");
            supervisor.setEmail("supervisor@quizapp.com");
            userRepository.save(supervisor);
            System.out.println("✓ Default SupervisorAdmin account created (Username: SupervisorAdmin, Password: 1234)");
        } else {
            System.out.println("✓ SupervisorAdmin account already exists");
        }

        // Seed subjects if empty
        if (subjectRepository.count() == 0) {
            String[] names = new String[] { "Front End", "Java", "Back End", "Laravel", "Databases", "Software Engineering" };
            for (String n : names) {
                Subject s = new Subject();
                s.setName(n);
                subjectRepository.save(s);
            }
            System.out.println("✓ Seeded default subjects");
        }
    }
}


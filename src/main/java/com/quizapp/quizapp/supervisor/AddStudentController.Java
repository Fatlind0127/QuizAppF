package com.quizapp.quizapp.supervisor;

import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizapp.quizapp.user.User;
import com.quizapp.quizapp.user.UserRepository;

@Controller
@RequestMapping("/supervisor")
public class AddStudentController {

    private static int idCounter = 1;
    private final SecureRandom random = new SecureRandom();
    private final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";

    private final UserRepository userRepository;

    public AddStudentController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/add-student")
    public String addStudentsToJsonAndDB(
            @RequestParam("className") String className,
            @RequestParam("name") List<String> names,
            @RequestParam("surname") List<String> surnames,
            Map<String, Object> model
    ) throws IOException {

        if (names.size() != surnames.size()) {
            model.put("error", "Names and surnames count do not match.");
            return "supervisor/add-student";
        }

        List<Map<String, Object>> studentsJsonList = new ArrayList<>();

        for (int i = 0; i < names.size(); i++) {
            String firstName = names.get(i).trim();
            String lastName = surnames.get(i).trim();
            if (firstName.isEmpty() || lastName.isEmpty()) continue;

            String username = (firstName.substring(0, 1).toLowerCase() + "." + lastName.toLowerCase()).replaceAll("\\s+", "");
            username = ensureUniqueUsername(username);
            String password = generateSecurePassword(8);


            User student = new User();
            student.setFullName(firstName + " " + lastName);
            student.setUsername(username);
            student.setPassword(password);
            student.setRole(User.Role.STUDENT);
            student.setEmail(null);
            student.setClassName(className);
            userRepository.save(student);


            Map<String, Object> studentJson = new HashMap<>();
            studentJson.put("ID", idCounter++);
            studentJson.put("Username", username);
            studentJson.put("Password", password);
            studentJson.put("Class", className);

            studentsJsonList.add(studentJson);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(new FileWriter("students.json"), studentsJsonList);

        model.put("message", "Students saved to database and students.json!");
        model.put("students", studentsJsonList);

        return "supervisor/add-student-result";
    }

    private String generateSecurePassword(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String ensureUniqueUsername(String base) {
        String candidate = base;
        int n = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + n;
            n++;
        }
        return candidate;
    }
}

package com.quizapp.quizapp.supervisor;

import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizapp.quizapp.auth.AuthService;
import com.quizapp.quizapp.domain.Quiz;
import com.quizapp.quizapp.repository.QuizRepository;
import com.quizapp.quizapp.user.User;
import com.quizapp.quizapp.user.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/supervisor")
public class SupervisorController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    

    private static int idCounter = 1;
    private final SecureRandom random = new SecureRandom();
    private final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";

    public SupervisorController(AuthService authService, UserRepository userRepository,
                                QuizRepository quizRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        
        System.out.println("SupervisorController initialized");
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }

        User currentUser = authService.getCurrentUser(session);
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        List<User> students = userRepository.findByRole(User.Role.STUDENT);
        List<Quiz> quizzes = quizRepository.findAll();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("admins", admins);
        model.addAttribute("students", students);
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("totalAdmins", admins.size());
        model.addAttribute("totalStudents", students.size());
        model.addAttribute("totalQuizzes", quizzes.size());

        return "supervisor-dashboard";
    }


    @GetMapping("/add-student")
    public String addStudentPage(HttpSession session) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        return "supervisor/add-student";
    }

    @PostMapping("/add-student")
    public String addStudentsToJsonAndDB(
            @RequestParam("className") String className,
            @RequestParam("name") List<String> names,
            @RequestParam("surname") List<String> surnames,
            Map<String, Object> model
    ) throws IOException {

        System.out.println("POST /add-student called");
        System.out.println("Class: " + className);
        System.out.println("Names: " + names);
        System.out.println("Surnames: " + surnames);

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

            System.out.println("Creating student: " + firstName + " " + lastName);
            System.out.println("Username: " + username + ", Password: " + password);

            User student = new User();
            student.setFullName(firstName + " " + lastName);
            student.setUsername(username);
            student.setPassword(password);
            student.setRole(User.Role.STUDENT);
            student.setEmail(null);
            student.setClassName(className);
            
            try {
                User saved = userRepository.save(student);
                System.out.println("Saved student with ID: " + saved.getId());
            } catch (Exception e) {
                System.err.println("Error saving student: " + e.getMessage());
                e.printStackTrace();
            }

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

    @GetMapping("/assign-users")
    public String assignUsersPage(HttpSession session) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        return "supervisor/assign-users";
    }


    @GetMapping("/manage-classes")
    public String manageClassesPage(HttpSession session) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        return "supervisor/manage-classes";
    }

    @GetMapping("/manage-users")
    public String manageUsersPage(HttpSession session) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        return "supervisor/manage-users";
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
package com.quizapp.quizapp.supervisor;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    public SupervisorController(AuthService authService, UserRepository userRepository, 
                               QuizRepository quizRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
    }

    // --- Dashboard ---
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

    // --- Create Admin ---
    @PostMapping("/create-admin")
    public String createAdmin(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam(required = false) String fullName,
                             @RequestParam(required = false) String email,
                             HttpSession session) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }

        if (userRepository.existsByUsername(username)) {
            return "redirect:/supervisor/dashboard?error=username_exists";
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(password);
        admin.setRole(User.Role.ADMIN);
        admin.setFullName(fullName);
        admin.setEmail(email);
        userRepository.save(admin);

        return "redirect:/supervisor/dashboard?success=admin_created";
    }

    // --- Create Student ---
    @PostMapping("/create-student")
    public String createStudent(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam(required = false) String fullName,
                               @RequestParam(required = false) String email,
                               HttpSession session) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }

        if (userRepository.existsByUsername(username)) {
            return "redirect:/supervisor/dashboard?error=username_exists";
        }

        User student = new User();
        student.setUsername(username);
        student.setPassword(password);
        student.setRole(User.Role.STUDENT);
        student.setFullName(fullName);
        student.setEmail(email);
        userRepository.save(student);

        return "redirect:/supervisor/dashboard?success=student_created";
    }

    // --- Delete User ---
    @PostMapping("/delete-user/{id}")
    public String deleteUser(@PathVariable Integer id, HttpSession session) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }

        User user = userRepository.findById(id).orElse(null);
        if (user != null && user.getRole() != User.Role.SUPERVISOR_ADMIN) {
            userRepository.deleteById(id);
            return "redirect:/supervisor/dashboard?success=user_deleted";
        }

        return "redirect:/supervisor/dashboard?error=cannot_delete";
    }

    // --- View All Users ---
    @GetMapping("/users")
    public String viewAllUsers(HttpSession session, Model model) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }

        List<User> allUsers = userRepository.findAll();
        model.addAttribute("users", allUsers);
        model.addAttribute("currentUser", authService.getCurrentUser(session));

        return "supervisor-users";
    }
}


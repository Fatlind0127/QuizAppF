package com.quizapp.quizapp.student;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.quizapp.quizapp.auth.AuthService;
import com.quizapp.quizapp.domain.Quiz;
import com.quizapp.quizapp.repository.QuizRepository;
import com.quizapp.quizapp.user.UserRepository;
import com.quizapp.quizapp.user.User;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final AuthService authService;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentController(AuthService authService, QuizRepository quizRepository,
                             UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // --- Home/Dashboard ---
    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        if (!authService.isAuthorized(session, User.Role.STUDENT)) {
            return "redirect:/?error=unauthorized";
        }

        User currentUser = authService.getCurrentUser(session);
        List<Quiz> quizzes = quizRepository.findAll();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("quizzes", quizzes);

        return "student-home";
    }

    // --- Account (Change Password) ---
    @GetMapping("/account")
    public String account(HttpSession session, Model model,
                          @RequestParam(value = "success", required = false) String success,
                          @RequestParam(value = "error", required = false) String error) {
        if (!authService.isAuthorized(session, User.Role.STUDENT)) {
            return "redirect:/?error=unauthorized";
        }
        model.addAttribute("currentUser", authService.getCurrentUser(session));
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        return "student-account";
    }

    @PostMapping("/account")
    public String changePassword(HttpSession session,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword) {
        if (!authService.isAuthorized(session, User.Role.STUDENT)) {
            return "redirect:/?error=unauthorized";
        }
        User user = authService.getCurrentUser(session);
        if (user == null) return "redirect:/";

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return "redirect:/student/account?error=invalid_current";
        }
        if (newPassword == null || newPassword.length() < 6) {
            return "redirect:/student/account?error=weak_password";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/student/account?error=nomatch";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return "redirect:/student/account?success=1";
    }

    // --- View Available Quizzes ---
    // @GetMapping("/quizzes")
    // public String viewQuizzes(HttpSession session, Model model) {
    //     if (!authService.isAuthorized(session, User.Role.STUDENT)) {
    //         return "redirect:/?error=unauthorized";
    //     }

    //     List<Quiz> quizzes = quizRepository.findAll();
    //     model.addAttribute("quizzes", quizzes);
    //     model.addAttribute("currentUser", authService.getCurrentUser(session));

    //     return "student-quizzes";
    // }
}


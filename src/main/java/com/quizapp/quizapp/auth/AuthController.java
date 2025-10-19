package com.quizapp.quizapp.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    // --- GET /  → show login page -------------------------
    @GetMapping("/")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                                Model model) {
        model.addAttribute("error", error != null);
        return "login";                     // loads templates/login.html
    }

    // --- POST /  → check credentials and redirect ---------
    @PostMapping("/")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password) {

        AuthService.Role role = auth.checkCredentials(username, password);

        switch (role) {
            case ADMIN:
                return "redirect:/admin/dashboard";
            case STUDENT:
                return "redirect:/student/home";
            default:
                return "redirect:/?error=1";
        }
    }

    // --- simple pages for testing redirects ---------------
    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin-dashboard";           // templates/admin-dashboard.html
    }

    @GetMapping("/student/home")
    public String studentHome() {
        return "student-home";              // templates/student-home.html
    }
}

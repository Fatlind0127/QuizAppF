package com.quizapp.quizapp.supervisor;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    // --- Pages for actions ---


@GetMapping("/add-student")
public String addStudentPage(HttpSession session) {
    if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
        return "redirect:/?error=unauthorized";
    }
    return "supervisor/add-student";
}

@GetMapping("/assign-users")
public String assignUsersPage(HttpSession session) {
    if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
        return "redirect:/?error=unauthorized";
    }
    return "supervisor/assign-users";
}

@GetMapping("/create-class")
public String createClassPage(HttpSession session) {
    if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
        return "redirect:/?error=unauthorized";
    }
    return "supervisor/create-class";
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

}

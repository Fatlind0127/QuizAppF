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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/supervisor")
public class SupervisorController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.quizapp.quizapp.subject.SubjectRepository subjectRepository;
    private final com.quizapp.quizapp.classassignment.ClassAssignmentRepository classAssignmentRepository;
    

    private static int idCounter = 1;
    private final SecureRandom random = new SecureRandom();
    private final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";

    public SupervisorController(AuthService authService, UserRepository userRepository,
                                QuizRepository quizRepository, PasswordEncoder passwordEncoder,
                                com.quizapp.quizapp.subject.SubjectRepository subjectRepository,
                                com.quizapp.quizapp.classassignment.ClassAssignmentRepository classAssignmentRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.passwordEncoder = passwordEncoder;
        this.subjectRepository = subjectRepository;
        this.classAssignmentRepository = classAssignmentRepository;
        
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
    public String addStudentPage(HttpSession session, Model model) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        model.addAttribute("admins", userRepository.findByRole(User.Role.ADMIN));
        model.addAttribute("subjects", subjectRepository.findAll());
        return "supervisor/add-student";
    }

    @PostMapping("/add-student")
    public String addStudentsToJsonAndDB(
            @RequestParam("className") String className,
            @RequestParam("name") List<String> names,
            @RequestParam("surname") List<String> surnames,
            @RequestParam("teacherId") Integer teacherId,
            @RequestParam("subjectId") Integer subjectId,
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

        // Ensure class assignment exists/updated for this class and subject
        User teacher = userRepository.findById(teacherId).orElse(null);
        var subjectOpt = subjectRepository.findById(subjectId);
        if (teacher == null || subjectOpt.isEmpty()) {
            model.put("error", "Invalid teacher or subject selection.");
            return "supervisor/add-student";
        }
        var subject = subjectOpt.get();
        var existingAssign = classAssignmentRepository.findByClassNameAndSubject_Id(className, subjectId).orElse(null);
        if (existingAssign == null) {
            var assign = new com.quizapp.quizapp.classassignment.ClassAssignment();
            assign.setClassName(className);
            assign.setTeacher(teacher);
            assign.setSubject(subject);
            classAssignmentRepository.save(assign);
        }

        for (int i = 0; i < names.size(); i++) {
            String firstName = names.get(i).trim();
            String lastName = surnames.get(i).trim();
            if (firstName.isEmpty() || lastName.isEmpty()) continue;

            String username = (firstName.substring(0, 1).toLowerCase() + "." + lastName.toLowerCase()).replaceAll("\\s+", "");
            username = ensureUniqueUsername(username);
            String password = buildStudentPassword(firstName, lastName);

            System.out.println("Creating student: " + firstName + " " + lastName);
            System.out.println("Username: " + username + ", Password: " + password);

            User student = new User();
            student.setFullName(firstName + " " + lastName);
            student.setUsername(username);
            student.setPassword(passwordEncoder.encode(password));
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
            studentJson.put("Subject", subject.getName());
            studentJson.put("Teacher", teacher.getUsername());

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


    @GetMapping("/list-students")
    public String listStudents(HttpSession session, Model model) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        model.addAttribute("users", userRepository.findByRole(User.Role.STUDENT));
        return "supervisor/list-students";
    }

    @GetMapping("/list-teachers")
    public String listTeachers(HttpSession session, Model model) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        model.addAttribute("users", userRepository.findByRole(User.Role.ADMIN));
        return "supervisor/list-teachers";
    }

    @PostMapping("/delete-user/{id}")
    public String deleteUser(HttpSession session, @PathVariable Integer id,
                             @RequestParam(value = "redirectTo", required = false) String redirectTo) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        try {
            userRepository.deleteById(id);
        } catch (Exception ignored) {}
        if (redirectTo != null && !redirectTo.isBlank()) {
            return "redirect:" + redirectTo;
        }
        return "redirect:/supervisor/dashboard";
    }

    @GetMapping("/edit-user/{id}")
    public String editUserForm(HttpSession session, @PathVariable Integer id, Model model,
                               @RequestParam(value = "backTo", required = false) String backTo) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return "redirect:/supervisor/dashboard";
        model.addAttribute("user", user);
        model.addAttribute("backTo", backTo != null ? backTo : "/supervisor/dashboard");
        return "supervisor/edit-user";
    }

    @PostMapping("/edit-user/{id}")
    public String editUserSubmit(HttpSession session, @PathVariable Integer id,
                                 @RequestParam String username,
                                 @RequestParam(required = false) String fullName,
                                 @RequestParam(required = false) String email,
                                 @RequestParam(required = false) String className,
                                 @RequestParam(required = false) String newPassword,
                                 @RequestParam(required = false) String confirmPassword,
                                 @RequestParam(value = "backTo", required = false) String backTo) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        userRepository.findById(id).ifPresent(u -> {
            u.setUsername(username);
            u.setFullName(fullName);
            u.setEmail(email);
            if (u.getRole() == User.Role.STUDENT) {
                u.setClassName(className);
            }
            // Update password if both fields are provided and match
            if (newPassword != null && !newPassword.isBlank() && 
                confirmPassword != null && !confirmPassword.isBlank()) {
                if (newPassword.equals(confirmPassword)) {
                    u.setPassword(passwordEncoder.encode(newPassword));
                }
            }
            userRepository.save(u);
        });
        String target = (backTo != null && !backTo.isBlank()) ? backTo : "/supervisor/dashboard";
        return "redirect:" + target;
    }

    // -------- Subjects management --------
    @GetMapping("/subjects")
    public String manageSubjects(HttpSession session, Model model) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        model.addAttribute("subjects", subjectRepository.findAll());
        return "supervisor/subjects";
    }

    @PostMapping("/subjects")
    public String addSubject(HttpSession session, @RequestParam String name) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        String n = name == null ? "" : name.trim();
        if (!n.isEmpty() && subjectRepository.findByName(n).isEmpty()) {
            var s = new com.quizapp.quizapp.subject.Subject();
            s.setName(n);
            subjectRepository.save(s);
        }
        return "redirect:/supervisor/subjects";
    }

    @GetMapping("/subjects/{id}/edit")
    public String editSubjectForm(HttpSession session, @PathVariable Integer id, Model model) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        var subject = subjectRepository.findById(id).orElse(null);
        if (subject == null) return "redirect:/supervisor/subjects";
        model.addAttribute("subject", subject);
        return "supervisor/edit-subject";
    }

    @PostMapping("/subjects/{id}/edit")
    public String editSubjectSubmit(HttpSession session, @PathVariable Integer id, @RequestParam String name) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        subjectRepository.findById(id).ifPresent(s -> { s.setName(name); subjectRepository.save(s); });
        return "redirect:/supervisor/subjects";
    }

    @PostMapping("/subjects/{id}/delete")
    public String deleteSubject(HttpSession session, @PathVariable Integer id) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        try { subjectRepository.deleteById(id); } catch (Exception ignored) {}
        return "redirect:/supervisor/subjects";
    }

    @GetMapping("/subjects/{id}/classes")
    public String subjectClasses(HttpSession session, @PathVariable Integer id, Model model) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        var subject = subjectRepository.findById(id).orElse(null);
        if (subject == null) return "redirect:/supervisor/subjects";
        var assigns = classAssignmentRepository.findAllBySubject_Id(id);
        java.util.List<java.util.Map<String,Object>> rows = new java.util.ArrayList<>();
        for (var a : assigns) {
            java.util.Map<String,Object> row = new java.util.HashMap<>();
            row.put("className", a.getClassName());
            row.put("teacher", a.getTeacher() != null ? a.getTeacher().getUsername() : "-");
            java.util.List<com.quizapp.quizapp.user.User> studs = userRepository.findByRole(com.quizapp.quizapp.user.User.Role.STUDENT);
            long count = studs.stream().filter(s -> a.getClassName().equals(s.getClassName())).count();
            row.put("studentCount", count);
            rows.add(row);
        }
        model.addAttribute("subject", subject);
        model.addAttribute("rows", rows);
        return "supervisor/subject-classes";
    }

    @GetMapping("/classes/{className}/students")
    public String viewStudentsInClass(HttpSession session, @PathVariable String className, Model model) {
        if (!authService.isAuthorized(session, User.Role.SUPERVISOR_ADMIN)) {
            return "redirect:/?error=unauthorized";
        }
        java.util.List<com.quizapp.quizapp.user.User> studs = userRepository.findByRole(com.quizapp.quizapp.user.User.Role.STUDENT);
        java.util.List<com.quizapp.quizapp.user.User> inClass = new java.util.ArrayList<>();
        for (var s : studs) {
            if (className.equals(s.getClassName())) inClass.add(s);
        }
        model.addAttribute("className", className);
        model.addAttribute("students", inClass);
        return "supervisor/class-students";
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

    private String buildStudentPassword(String firstName, String lastName) {
        String f = firstName == null ? "" : firstName.trim().toLowerCase();
        String l = lastName == null ? "" : lastName.trim().toLowerCase();
        String lInitial = l.isEmpty() ? (f.isEmpty() ? "s" : f.substring(0,1)) : l.substring(0,1);
        return lInitial + "." + f;
    }
}
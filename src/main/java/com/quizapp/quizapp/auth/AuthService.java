package com.quizapp.quizapp.auth;

import com.quizapp.quizapp.admin.Admin;
import com.quizapp.quizapp.admin.AdminRepository;
import com.quizapp.quizapp.student.Student;
import com.quizapp.quizapp.student.StudentRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AdminRepository adminRepo;
    private final StudentRepository studentRepo;

    public AuthService(AdminRepository adminRepo, StudentRepository studentRepo) {
        this.adminRepo = adminRepo;
        this.studentRepo = studentRepo;
    }

    public Role checkCredentials(String username, String password) {
        // 1) Check admin
        Admin admin = adminRepo.findByUsername(username).orElse(null);
        if (admin != null && admin.getPassword().equals(password)) {
            return Role.ADMIN;
        }
        // 2) Check student
        Student student = studentRepo.findByUsername(username).orElse(null);
        if (student != null && student.getPassword().equals(password)) {
            return Role.STUDENT;
        }
        // 3) No match
        return Role.NONE;
    }

    public enum Role { ADMIN, STUDENT, NONE }
}

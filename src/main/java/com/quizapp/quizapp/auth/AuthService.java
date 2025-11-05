package com.quizapp.quizapp.auth;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.quizapp.quizapp.user.User;
import com.quizapp.quizapp.user.UserRepository;

import jakarta.servlet.http.HttpSession;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return null;

        String stored = user.getPassword();
        boolean isHashed = stored != null && stored.startsWith("$2");

        if (isHashed) {
            if (passwordEncoder.matches(password, stored)) {
                return user;
            }
            return null;
        }

        // Backward-compat: if stored is plaintext and matches, upgrade to hash once
        if (stored != null && stored.equals(password)) {
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
            return user;
        }
        return null;
    }

    public void setCurrentUser(HttpSession session, User user) {
        session.setAttribute("currentUser", user);
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole().toString());
    }

    public User getCurrentUser(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId != null) {
            return userRepository.findById(userId).orElse(null);
        }
        return null;
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public boolean isAuthorized(HttpSession session, User.Role... allowedRoles) {
        User user = getCurrentUser(session);
        if (user == null) return false;
        for (User.Role role : allowedRoles) {
            if (user.getRole() == role) return true;
        }
        return false;
    }
}

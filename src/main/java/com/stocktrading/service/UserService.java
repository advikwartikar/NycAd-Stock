package com.stocktrading.service;

import com.stocktrading.model.User;
import com.stocktrading.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ===============================
    // BASIC FETCH METHODS
    // ===============================

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getAllRegularUsers() {
        return userRepository.findByRole("USER");
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // ===============================
    // CREATE USER (FULL VERSION)
    // ===============================

    public User createUser(String username,
                           String password,
                           String fullName,
                           String email,
                           String role) {

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        user.setCredits(100000.0);
        user.setActive(true);

        return userRepository.save(user);
    }

    // ===============================
    // OVERLOADED CREATE USER (Controller Version - 4 params)
    // ===============================

    public User createUser(String username,
                           String password,
                           String fullName,
                           String email) {

        return createUser(username, password, fullName, email, "USER");
    }

    // ===============================
    // UPDATE USER (Original Version)
    // ===============================

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    // ===============================
    // OVERLOADED UPDATE USER (AdminController Version)
    // ===============================

    public User updateUser(Long id,
                           String fullName,
                           String email,
                           String password,
                           Double credits) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFullName(fullName);
        user.setEmail(email);
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        if (credits != null) {
            user.setCredits(credits);
        }

        return userRepository.save(user);
    }

    // ===============================
    // UPDATE CREDITS
    // ===============================

    public User updateCredits(Long userId, Double amount) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setCredits(user.getCredits() + amount);
        return userRepository.save(user);
    }

    public boolean hasEnoughCredits(User user, Double amount) {
        return user.getCredits() >= amount;
    }

    // ===============================
    // TOGGLE USER STATUS
    // ===============================

    public void toggleUserStatus(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(!user.getActive());
        userRepository.save(user);
    }

    // ===============================
    // DELETE USER
    // ===============================

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}

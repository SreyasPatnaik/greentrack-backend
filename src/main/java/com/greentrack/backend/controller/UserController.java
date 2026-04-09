package com.greentrack.backend.controller;

import com.greentrack.backend.model.User;
import com.greentrack.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    // In-memory mock store for OTPs (Demo only)
    private static ConcurrentHashMap<String, String> otpStore = new ConcurrentHashMap<>();

    @PostMapping("/send-otp")
    public Map<String, Object> sendOtp(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String email = request.get("email");
        
        if (email == null || email.isBlank()) {
            response.put("status", false);
            response.put("message", "Email is required");
            return response;
        }

        // Check if already registered
        if (userRepository.findByEmail(email).isPresent()) {
            response.put("status", false);
            response.put("message", "Email already registered!");
            return response;
        }

        // Generate 6-digit Mock OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStore.put(email, otp);
        
        // Mock sending by logging it to the console
        System.out.println("==================================================");
        System.out.println("MOCK OTP FOR " + email + " IS: " + otp);
        System.out.println("==================================================");

        response.put("status", true);
        response.put("message", "OTP sent via email!");
        return response;
    }

    @PostMapping("/verify-otp")
    public Map<String, Object> verifyOtp(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String email = request.get("email");
        String otp = request.get("otp");

        String storedOtp = otpStore.get(email);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStore.remove(email); // consume OTP
            response.put("status", true);
            response.put("message", "OTP Verified!");
        } else {
            response.put("status", false);
            response.put("message", "Invalid or expired OTP");
        }
        return response;
    }

    @PostMapping("/signup")
    public Map<String, Object> signup(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            response.put("message", "Email already registered!");
            response.put("status", false);
            return response;
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        response.put("message", "Signup successful!");
        response.put("status", true);
        response.put("user", user); // frontend ko user info milega
        response.put("id", user.getId());
        return response;
    }


    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginRequest) {
        Map<String, Object> response = new HashMap<>();

        String identifier = loginRequest.get("identifier");
        String password = loginRequest.get("password");

        if (identifier == null || password == null) {
            response.put("message", "Please provide both identifier and password!");
            response.put("status", false);
            return response;
        }

        // Try both email and contactNumber variations
        User user = userRepository.findByEmailOrContactNumber(identifier, identifier).orElse(null);

        // If not found, try adding +91
        if (user == null && identifier.matches("^\\d{10}$")) {
            user = userRepository.findByEmailOrContactNumber("+91" + identifier, "+91" + identifier).orElse(null);
        }

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            response.put("message", "Invalid email/contact or password!");
            response.put("status", false);
            return response;
        }

        response.put("message", "Login successful!");
        response.put("status", true);
        response.put("id", user.getId());
        response.put("user", user.getFullName());
        response.put("email", user.getEmail());
        response.put("contactNumber", user.getContactNumber());
        return response;
    }

    // =============================
    // Forgot Password API
    // This API verifies both email and contact number, then updates password in MySQL.
    // =============================
    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPassword(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        String email = request.get("email");
        String contactNumber = request.get("contactNumber");
        String newPassword = request.get("newPassword");

        // Validate input
        if (email == null || email.isBlank() ||
                contactNumber == null || contactNumber.isBlank() ||
                newPassword == null || newPassword.isBlank()) {
            response.put("status", false);
            response.put("message", "Please provide email, contact number, and new password!");
            return response;
        }

        // Normalize contact number format
        String normalizedContact = contactNumber.trim();
        if (!normalizedContact.startsWith("+91") && normalizedContact.matches("^\\d{10}$")) {
            normalizedContact = "+91" + normalizedContact;
        }

        // Find user by both email and contact number
        User user = userRepository.findByEmailAndContactNumber(email, normalizedContact).orElse(null);

        // Try without +91 if not found
        if (user == null && normalizedContact.startsWith("+91")) {
            String plainContact = normalizedContact.substring(3);
            user = userRepository.findByEmailAndContactNumber(email, plainContact).orElse(null);
        }

        // Fallback: Try finding by contactNumber alone (for phone-based recovery)
        if (user == null) {
            user = userRepository.findByContactNumber(normalizedContact).orElse(null);

            // Also try with +91 prefix
            if (user == null && !normalizedContact.startsWith("+91")) {
                user = userRepository.findByContactNumber("+91" + normalizedContact).orElse(null);
            }
            
            // Also try without +91 prefix
            if (user == null && normalizedContact.startsWith("+91")) {
                user = userRepository.findByContactNumber(normalizedContact.substring(3)).orElse(null);
            }
        }

        // If still not found, send error
        if (user == null) {
            response.put("status", false);
            response.put("message", "Email and contact number do not match any registered user!");
            return response;
        }

        // Update password (hashed)
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        response.put("status", true);
        response.put("message", "Password updated successfully!");
        return response;
    }

    // =============================
    // Get User Profile (public)
    // =============================
    @GetMapping("/profile/{userId}")
    public Map<String, Object> getProfile(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.put("status", false);
            response.put("message", "User not found");
            return response;
        }
        response.put("status", true);
        response.put("id", user.getId());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("contactNumber", user.getContactNumber());
        response.put("greenCoins", user.getGreenCoins());
        response.put("age", user.getAge());
        response.put("gender", user.getGender());
        response.put("profession", user.getProfession());
        response.put("city", user.getCity());
        response.put("bio", user.getBio());
        response.put("profileImageBase64", user.getProfileImageBase64());
        response.put("volunteerBadge", user.isVolunteerBadge());
        return response;
    }

    // =============================
    // Update User Profile
    // =============================
    @PutMapping("/profile/{userId}")
    public Map<String, Object> updateProfile(@PathVariable Long userId, @RequestBody Map<String, Object> profileData) {
        Map<String, Object> response = new HashMap<>();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.put("status", false);
            response.put("message", "User not found");
            return response;
        }

        if (profileData.containsKey("fullName")) user.setFullName((String) profileData.get("fullName"));
        if (profileData.containsKey("age")) {
            Object ageObj = profileData.get("age");
            if (ageObj instanceof Number) user.setAge(((Number) ageObj).intValue());
        }
        if (profileData.containsKey("gender")) user.setGender((String) profileData.get("gender"));
        if (profileData.containsKey("profession")) user.setProfession((String) profileData.get("profession"));
        if (profileData.containsKey("city")) user.setCity((String) profileData.get("city"));
        if (profileData.containsKey("bio")) user.setBio((String) profileData.get("bio"));
        if (profileData.containsKey("profileImageBase64")) user.setProfileImageBase64((String) profileData.get("profileImageBase64"));

        userRepository.save(user);

        response.put("status", true);
        response.put("message", "Profile updated successfully!");
        return response;
    }
}



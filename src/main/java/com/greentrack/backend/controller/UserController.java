package com.greentrack.backend.controller;

import com.greentrack.backend.model.User;
import com.greentrack.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WasteReportRepository wasteReportRepository;

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    @Autowired
    private CourseCompletionRepository courseCompletionRepository;

    @Autowired
    private OrderRepository orderRepository;

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

        // Normalize identifier
        String trimmedId = identifier.trim();

        // Try exact match first (email or stored phone)
        User user = userRepository.findByEmailOrContactNumber(trimmedId, trimmedId).orElse(null);

        // If not found, try phone number variations
        if (user == null) {
            if (trimmedId.matches("^\\d{10}$")) {
                // Try with +91 prefix (most common stored format)
                user = userRepository.findByEmailOrContactNumber("+91" + trimmedId, "+91" + trimmedId).orElse(null);
            } else if (trimmedId.startsWith("+91") && trimmedId.length() == 13) {
                // Stored without +91
                user = userRepository.findByEmailOrContactNumber(trimmedId.substring(3), trimmedId.substring(3)).orElse(null);
            } else if (trimmedId.startsWith("91") && trimmedId.length() == 12) {
                String plain = trimmedId.substring(2);
                user = userRepository.findByEmailOrContactNumber("+" + trimmedId, "+" + trimmedId).orElse(null);
                if (user == null) {
                    user = userRepository.findByEmailOrContactNumber(plain, plain).orElse(null);
                }
            }
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

        String identifier = request.get("identifier");
        String newPassword = request.get("newPassword");

        // Validate input
        if (identifier == null || identifier.isBlank() ||
                newPassword == null || newPassword.isBlank()) {
            response.put("status", false);
            response.put("message", "Please provide identifier (email or phone) and new password!");
            return response;
        }

        // Find user by either email or contact number
        User user = userRepository.findByEmailOrContactNumber(identifier, identifier).orElse(null);

        // If not found, try adding or removing +91 prefix for phone numbers
        if (user == null) {
            String normalizedContact = identifier.trim();
            if (normalizedContact.matches("^\\d{10}$")) {
                user = userRepository.findByEmailOrContactNumber("+91" + normalizedContact, "+91" + normalizedContact).orElse(null);
            } else if (normalizedContact.startsWith("+91")) {
                String plainContact = normalizedContact.substring(3);
                user = userRepository.findByEmailOrContactNumber(plainContact, plainContact).orElse(null);
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
        response.put("coverImageBase64", user.getCoverImageBase64());
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
        if (profileData.containsKey("coverImageBase64")) user.setCoverImageBase64((String) profileData.get("coverImageBase64"));

        userRepository.save(user);

        response.put("status", true);
        response.put("message", "Profile updated successfully!");
        return response;
    }

    // =============================
    // ADMIN: Management APIs
    // =============================

    @GetMapping("/admin/users/all")
    public List<User> getAllUsers() {
        // Simple fetch for admin — usually, you'd add role checks here
        return userRepository.findAll();
    }

    @DeleteMapping("/admin/users/{id}")
    @Transactional
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        if (!userRepository.existsById(id)) {
            response.put("status", false);
            response.put("message", "User not found");
            return response;
        }

        try {
            // Cascade delete all associated data to keep database clean
            wasteReportRepository.deleteByUserId(id);
            coinTransactionRepository.deleteByUserId(id);
            courseCompletionRepository.deleteByUserId(id);
            orderRepository.deleteByUserId(id);
            
            // Finally delete the user
            userRepository.deleteById(id);

            response.put("status", true);
            response.put("message", "User and all associated data deleted successfully!");
        } catch (Exception e) {
            response.put("status", false);
            response.put("message", "Error deleting user: " + e.getMessage());
        }
        
        return response;
    }
}



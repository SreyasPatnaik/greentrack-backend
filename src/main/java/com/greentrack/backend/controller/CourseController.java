package com.greentrack.backend.controller;

import com.greentrack.backend.model.CourseCompletion;
import com.greentrack.backend.model.CoinTransaction;
import com.greentrack.backend.model.User;
import com.greentrack.backend.repository.CourseCompletionRepository;
import com.greentrack.backend.repository.CoinTransactionRepository;
import com.greentrack.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin("*")
public class CourseController {

    @Autowired
    private CourseCompletionRepository courseCompletionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    @PostMapping("/complete")
    public ResponseEntity<?> completeCourse(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            Integer courseId = Integer.valueOf(payload.get("courseId").toString());
            Integer score = Integer.valueOf(payload.get("score").toString());
            Integer rewardCoins = Integer.valueOf(payload.get("rewardCoins").toString());
            String courseTitle = (String) payload.get("courseTitle");

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }

            // Check if already completed
            Optional<CourseCompletion> existing = courseCompletionRepository.findByUserIdAndCourseId(userId, courseId);
            if (existing.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "message", "Course already completed previously. No new coins awarded.",
                    "status", "ALREADY_COMPLETED",
                    "newBalance", user.getGreenCoins()
                ));
            }

            // Record completion
            CourseCompletion completion = new CourseCompletion(user, courseId, score, rewardCoins);
            courseCompletionRepository.save(completion);

            // Award coins
            user.setGreenCoins(user.getGreenCoins() + rewardCoins);
            userRepository.save(user);

            // Create transaction record
            CoinTransaction transaction = new CoinTransaction();
            transaction.setUser(user);
            transaction.setAmount(rewardCoins);
            transaction.setReason("Course Completion: " + (courseTitle != null ? courseTitle : "Course #" + courseId));
            transaction.setTimestamp(LocalDateTime.now());
            coinTransactionRepository.save(transaction);

            return ResponseEntity.ok(Map.of(
                "message", "Congratulations! Course completed and coins awarded.",
                "status", "SUCCESS",
                "coinsEarned", rewardCoins,
                "newBalance", user.getGreenCoins()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Internal server error", "error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserCompletions(@PathVariable Long userId) {
        List<CourseCompletion> completions = courseCompletionRepository.findByUserId(userId);
        List<Integer> completedCourseIds = completions.stream()
                .map(CourseCompletion::getCourseId)
                .collect(Collectors.toList());
        return ResponseEntity.ok(completedCourseIds);
    }
}

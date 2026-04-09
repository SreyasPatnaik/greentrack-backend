package com.greentrack.backend.controller;

import com.greentrack.backend.model.User;
import com.greentrack.backend.repository.UserRepository;
import com.greentrack.backend.repository.WasteReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin("*")
public class LeaderboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WasteReportRepository wasteReportRepository;

    @GetMapping
    public List<Map<String, Object>> getLeaderboard() {
        List<User> allUsers = userRepository.findAll();

        // Build leaderboard entries with approved report counts
        List<Map<String, Object>> leaderboard = new ArrayList<>();

        for (User user : allUsers) {
            Long approvedCount = wasteReportRepository.countByUserIdAndStatus(user.getId(), "APPROVED");
            if (approvedCount == null) approvedCount = 0L;

            Map<String, Object> entry = new HashMap<>();
            entry.put("userId", user.getId());
            entry.put("fullName", user.getFullName());
            entry.put("approvedReports", approvedCount);
            entry.put("greenCoins", user.getGreenCoins());
            entry.put("volunteerBadge", user.isVolunteerBadge());
            entry.put("city", user.getCity());
            entry.put("profession", user.getProfession());
            entry.put("profileImageBase64", user.getProfileImageBase64());
            leaderboard.add(entry);
        }

        // Sort by approved reports (descending), then by greenCoins (descending)
        leaderboard.sort((a, b) -> {
            int cmp = Long.compare((Long) b.get("approvedReports"), (Long) a.get("approvedReports"));
            if (cmp != 0) return cmp;
            return Integer.compare((Integer) b.get("greenCoins"), (Integer) a.get("greenCoins"));
        });

        // Assign ranks and handle volunteer badge logic
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).put("rank", i + 1);
        }

        // Volunteer badge logic: check if rank 1 user has been #1 for 7+ days
        if (!leaderboard.isEmpty()) {
            Long rank1UserId = (Long) leaderboard.get(0).get("userId");
            Long rank1Reports = (Long) leaderboard.get(0).get("approvedReports");

            // Only process if they have at least 1 approved report
            if (rank1Reports > 0) {
                User rank1User = userRepository.findById(rank1UserId).orElse(null);
                if (rank1User != null) {
                    if (rank1User.getRank1Since() == null) {
                        // First time at rank 1, set the timestamp
                        rank1User.setRank1Since(LocalDateTime.now());
                        userRepository.save(rank1User);
                    } else if (!rank1User.isVolunteerBadge()) {
                        // Check if they've been rank 1 for 7+ days
                        if (rank1User.getRank1Since().plusDays(7).isBefore(LocalDateTime.now())) {
                            rank1User.setVolunteerBadge(true);
                            userRepository.save(rank1User);
                            leaderboard.get(0).put("volunteerBadge", true);
                        }
                    }
                }
            }

            // Reset rank1Since for users who are no longer rank 1
            for (int i = 1; i < leaderboard.size(); i++) {
                Long userId = (Long) leaderboard.get(i).get("userId");
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.getRank1Since() != null) {
                    user.setRank1Since(null);
                    userRepository.save(user);
                }
            }
        }

        return leaderboard;
    }
}

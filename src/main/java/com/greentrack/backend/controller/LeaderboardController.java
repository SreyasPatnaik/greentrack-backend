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

        // ✅ OPTIMIZED: Fetch ALL approved report counts in a SINGLE query (eliminates N+1)
        List<Object[]> countResults = wasteReportRepository.countApprovedReportsGroupedByUser();
        Map<Long, Long> approvedCountMap = new HashMap<>();
        for (Object[] row : countResults) {
            Long userId = (Long) row[0];
            Long count = (Long) row[1];
            approvedCountMap.put(userId, count);
        }

        // ✅ Fetch total report counts (all statuses) for display
        List<Object[]> totalResults = wasteReportRepository.countAllReportsGroupedByUser();
        Map<Long, Long> totalCountMap = new HashMap<>();
        for (Object[] row : totalResults) {
            Long userId = (Long) row[0];
            Long count = (Long) row[1];
            totalCountMap.put(userId, count);
        }

        // Build leaderboard entries — no per-user DB query needed
        List<Map<String, Object>> leaderboard = new ArrayList<>();

        for (User user : allUsers) {
            Long approvedCount = approvedCountMap.getOrDefault(user.getId(), 0L);
            Long totalReports = totalCountMap.getOrDefault(user.getId(), 0L);

            Map<String, Object> entry = new HashMap<>();
            entry.put("userId", user.getId());
            entry.put("fullName", user.getFullName());
            entry.put("approvedReports", approvedCount);
            entry.put("totalReports", totalReports);
            entry.put("greenCoins", user.getGreenCoins());
            entry.put("volunteerBadge", user.isVolunteerBadge());
            entry.put("city", user.getCity());
            entry.put("profession", user.getProfession());
            entry.put("profileImageBase64", user.getProfileImageBase64());
            leaderboard.add(entry);
        }

        // Sort by greenCoins (descending) — coins represent actual earned contributions
        leaderboard.sort((a, b) -> {
            int cmp = Integer.compare((Integer) b.get("greenCoins"), (Integer) a.get("greenCoins"));
            if (cmp != 0) return cmp;
            return Long.compare((Long) b.get("totalReports"), (Long) a.get("totalReports"));
        });

        // Assign ranks
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

            // ✅ OPTIMIZED: Only reset rank1Since for users who actually have it set
            // Batch collect IDs first, then update only those that need it
            List<Long> userIdsToReset = new ArrayList<>();
            for (int i = 1; i < leaderboard.size(); i++) {
                userIdsToReset.add((Long) leaderboard.get(i).get("userId"));
            }

            if (!userIdsToReset.isEmpty()) {
                List<User> usersToCheck = userRepository.findAllById(userIdsToReset);
                List<User> usersToSave = new ArrayList<>();
                for (User user : usersToCheck) {
                    if (user.getRank1Since() != null) {
                        user.setRank1Since(null);
                        usersToSave.add(user);
                    }
                }
                if (!usersToSave.isEmpty()) {
                    userRepository.saveAll(usersToSave);
                }
            }
        }

        return leaderboard;
    }
}

package com.greentrack.backend.controller;

import com.greentrack.backend.model.CoinTransaction;
import com.greentrack.backend.model.User;
import com.greentrack.backend.model.WasteReport;
import com.greentrack.backend.repository.CoinTransactionRepository;
import com.greentrack.backend.repository.UserRepository;
import com.greentrack.backend.repository.WasteReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin("*")
public class WasteReportController {

    @Autowired
    private WasteReportRepository wasteReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    public static class ReportRequest {
        public String userId;
        public String wasteType;
        public String severity;
        public String description;
        public String locationData;
        public String imageBase64;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitReport(@RequestBody ReportRequest request) {
        User user = null;
        if (request.userId != null && !request.userId.trim().isEmpty() && !request.userId.equals("undefined")) {
            try {
                Long uid = Long.parseLong(request.userId);
                user = userRepository.findById(uid).orElse(null);
                if (user == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid user ID string: " + request.userId);
            }
        }

        WasteReport report = new WasteReport();
        report.setUser(user);
        report.setWasteType(request.wasteType);
        report.setSeverity(request.severity);
        report.setDescription(request.description);
        report.setLocationData(request.locationData);
        report.setImageBase64(request.imageBase64);
        report.setStatus("PENDING");

        int coinsToAward = 20; // Default
        String type = request.wasteType != null ? request.wasteType.toLowerCase() : "";
        if (type.contains("plastic")) coinsToAward = 30;
        else if (type.contains("e-waste") || type.contains("medical")) coinsToAward = 50;
        else if (type.contains("dumping") || type.contains("high") || (request.severity != null && request.severity.toLowerCase().contains("high"))) coinsToAward = 75;
        
        report.setCoinsEarned(coinsToAward);
        wasteReportRepository.save(report);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Report submitted for Verification");
        response.put("coinsExpected", coinsToAward);
        response.put("status", "PENDING");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserReports(@PathVariable Long userId) {
        List<WasteReport> reports = wasteReportRepository.findByUserIdOrderByTimestampDesc(userId);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/admin/pending")
    public ResponseEntity<?> getPendingReports() {
        List<WasteReport> reports = wasteReportRepository.findByStatusOrderByTimestampDesc("PENDING");
        return ResponseEntity.ok(reports);
    }

    @PostMapping("/admin/approve/{reportId}")
    public ResponseEntity<?> approveReport(@PathVariable Long reportId) {
        WasteReport report = wasteReportRepository.findById(reportId).orElse(null);
        if (report == null) return ResponseEntity.badRequest().body(Map.of("message", "Report not found"));
        if (!"PENDING".equals(report.getStatus())) return ResponseEntity.badRequest().body(Map.of("message", "Already processed"));

        report.setStatus("APPROVED");
        report.setCleanupStatus("PROCESSING");
        wasteReportRepository.save(report);

        User user = report.getUser();
        if (user != null) {
            user.setGreenCoins(user.getGreenCoins() + report.getCoinsEarned());
            userRepository.save(user);

            CoinTransaction transaction = new CoinTransaction();
            transaction.setUser(user);
            transaction.setAmount(report.getCoinsEarned());
            transaction.setReason("Report Verification Approved: " + report.getWasteType());
            transaction.setTimestamp(LocalDateTime.now());
            coinTransactionRepository.save(transaction);
        }

        return ResponseEntity.ok(Map.of("message", "Report Approved"));
    }

    @PostMapping("/admin/reject/{reportId}")
    public ResponseEntity<?> rejectReport(@PathVariable Long reportId) {
        WasteReport report = wasteReportRepository.findById(reportId).orElse(null);
        if (report == null) return ResponseEntity.badRequest().body(Map.of("message", "Report not found"));
        if (!"PENDING".equals(report.getStatus())) return ResponseEntity.badRequest().body(Map.of("message", "Already processed"));

        report.setStatus("REJECTED");
        wasteReportRepository.save(report);

        return ResponseEntity.ok(Map.of("message", "Report Rejected"));
    }

    @GetMapping("/admin/approved")
    public ResponseEntity<?> getApprovedReports() {
        List<WasteReport> reports = wasteReportRepository.findByStatusOrderByTimestampDesc("APPROVED");
        return ResponseEntity.ok(reports);
    }

    public static class CleanupStatusRequest {
        public String cleanupStatus;
    }

    @PostMapping("/admin/cleanup-status/{reportId}")
    public ResponseEntity<?> updateCleanupStatus(@PathVariable Long reportId, @RequestBody CleanupStatusRequest request) {
        WasteReport report = wasteReportRepository.findById(reportId).orElse(null);
        if (report == null) return ResponseEntity.badRequest().body(Map.of("message", "Report not found"));
        if (!"APPROVED".equals(report.getStatus())) return ResponseEntity.badRequest().body(Map.of("message", "Report is not approved"));

        String newStatus = request.cleanupStatus;
        if (!"PROCESSING".equals(newStatus) && !"ASSIGNED".equals(newStatus) && !"CLEANED".equals(newStatus)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid cleanup status"));
        }

        report.setCleanupStatus(newStatus);
        wasteReportRepository.save(report);

        return ResponseEntity.ok(Map.of("message", "Cleanup status updated to " + newStatus));
    }

    // ✅ NEW: Delete a waste report (cleanup workflow delete + reduce DB load)
    @DeleteMapping("/{reportId}")
    @Transactional
    public ResponseEntity<?> deleteReport(@PathVariable Long reportId, @RequestParam Long userId) {
        WasteReport report = wasteReportRepository.findById(reportId).orElse(null);
        if (report == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Report not found"));
        }

        // Validate ownership — the requesting user must own this report
        User reportOwner = report.getUser();
        if (reportOwner == null || !reportOwner.getId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Unauthorized: You can only delete your own reports"));
        }

        // If the report was approved and coins were awarded, deduct them
        if ("APPROVED".equals(report.getStatus()) && report.getCoinsEarned() != null && report.getCoinsEarned() > 0) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                int newBalance = Math.max(0, user.getGreenCoins() - report.getCoinsEarned());
                user.setGreenCoins(newBalance);
                userRepository.save(user);

                // Delete associated coin transaction(s) for this report type
                List<CoinTransaction> relatedTransactions = coinTransactionRepository
                    .findByUserIdAndReasonContaining(userId, report.getWasteType());
                if (!relatedTransactions.isEmpty()) {
                    // Delete the first matching transaction (most specific match)
                    coinTransactionRepository.delete(relatedTransactions.get(0));
                }
            }
        }

        // Delete the report
        wasteReportRepository.delete(report);

        return ResponseEntity.ok(Map.of(
            "message", "Report deleted successfully",
            "coinsDeducted", ("APPROVED".equals(report.getStatus()) ? report.getCoinsEarned() : 0)
        ));
    }
}

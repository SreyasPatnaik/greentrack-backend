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
        public Long userId;
        public String wasteType;
        public String severity;
        public String description;
        public String locationData;
        public String imageBase64;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitReport(@RequestBody ReportRequest request) {
        User user = null;
        if (request.userId != null) {
            user = userRepository.findById(request.userId).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
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
}

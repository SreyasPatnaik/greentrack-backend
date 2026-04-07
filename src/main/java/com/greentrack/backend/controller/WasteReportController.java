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
@CrossOrigin(origins = "http://localhost:3000")
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
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitReport(@RequestBody ReportRequest request) {
        if (request.userId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "User ID is required for non-anonymous reports"));
        }

        User user = userRepository.findById(request.userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }

        // Create and save WasteReport
        WasteReport report = new WasteReport();
        report.setUser(user);
        report.setWasteType(request.wasteType);
        report.setSeverity(request.severity);
        report.setDescription(request.description);
        report.setLocationData(request.locationData);
        wasteReportRepository.save(report);

        // Determine coins based on waste type
        int coinsToAward = 20; // Default
        String type = request.wasteType != null ? request.wasteType.toLowerCase() : "";
        if (type.contains("plastic")) coinsToAward = 30;
        else if (type.contains("e-waste") || type.contains("medical")) coinsToAward = 50;
        else if (type.contains("dumping") || type.contains("high")) coinsToAward = 75;

        // Update user's coin balance
        user.setGreenCoins(user.getGreenCoins() + coinsToAward);
        userRepository.save(user);

        // Record the transaction
        CoinTransaction transaction = new CoinTransaction();
        transaction.setUser(user);
        transaction.setAmount(coinsToAward);
        transaction.setReason("Reported " + request.wasteType + " Waste");
        transaction.setTimestamp(LocalDateTime.now());
        coinTransactionRepository.save(transaction);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Report submitted successfully");
        response.put("coinsEarned", coinsToAward);
        response.put("newTotalCoins", user.getGreenCoins());
        return ResponseEntity.ok(response);
    }
}

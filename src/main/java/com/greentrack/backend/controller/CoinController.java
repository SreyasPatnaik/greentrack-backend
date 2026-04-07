package com.greentrack.backend.controller;

import com.greentrack.backend.model.CoinTransaction;
import com.greentrack.backend.model.User;
import com.greentrack.backend.repository.CoinTransactionRepository;
import com.greentrack.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coins")
@CrossOrigin(origins = "http://localhost:3000")
public class CoinController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getCoinHistory(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }

        List<CoinTransaction> history = coinTransactionRepository.findByUserIdOrderByTimestampDesc(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("totalCoins", user.getGreenCoins());
        response.put("history", history);

        return ResponseEntity.ok(response);
    }
}

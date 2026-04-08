package com.greentrack.backend.controller;

import com.greentrack.backend.model.Order;
import com.greentrack.backend.model.User;
import com.greentrack.backend.repository.OrderRepository;
import com.greentrack.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            String orderItemsJson = payload.get("orderItemsJson").toString();
            Double totalAmount = Double.valueOf(payload.get("totalAmount").toString());
            Integer coinsUsed = Integer.valueOf(payload.get("coinsUsed").toString());
            Double discountApplied = Double.valueOf(payload.get("discountApplied").toString());

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }

            Order order = new Order();
            order.setUser(userOpt.get());
            order.setOrderItemsJson(orderItemsJson);
            order.setTotalAmount(totalAmount);
            order.setCoinsUsed(coinsUsed);
            order.setDiscountApplied(discountApplied);
            order.setStatus("CONFIRMED");
            order.setCreatedAt(LocalDateTime.now());

            Order savedOrder = orderRepository.save(order);
            return ResponseEntity.ok(savedOrder);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to create order: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(@PathVariable Long userId) {
        try {
            List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to fetch orders: " + e.getMessage());
        }
    }
}

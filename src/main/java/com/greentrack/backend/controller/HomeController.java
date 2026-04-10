package com.greentrack.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class HomeController {

    @Autowired
    private com.greentrack.backend.repository.UserRepository userRepository;

    @GetMapping("/")
    public String home() {
        try {
            userRepository.findById(-1L);
        } catch(Exception e) {
            // suppress ping errors
        }
        return "GreenTrack Backend is running successfully!";
    }
}

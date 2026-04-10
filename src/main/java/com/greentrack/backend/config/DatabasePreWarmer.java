package com.greentrack.backend.config;

import com.greentrack.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DatabasePreWarmer {

    @Autowired
    private UserRepository userRepository;

    /**
     * Executes immediately after the application has fully started.
     * This forces the database connection pool (Hikari) to eagerly establish a connection
     * and forces Hibernate to evaluate all metamodels, preventing the 2-minute 
     * lazy-initialization delay on first frontend login.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        System.out.println("=========================================");
        System.out.println("🔥 ApplicationReady: Pre-warming Database Data Source & Hibernate Models...");
        try {
            // A simple fast query that forces the underlying connection open
            userRepository.findById(-1L);
            System.out.println("✅ Database connection pool is fully warmed up and ready.");
        } catch (Exception e) {
            System.err.println("⚠️ Database Pre-warmer failed: " + e.getMessage());
        }
        System.out.println("=========================================");
    }
}

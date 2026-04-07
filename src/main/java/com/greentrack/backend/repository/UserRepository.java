package com.greentrack.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.greentrack.backend.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailOrContactNumber(String email, String contactNumber);
    Optional<User> findByEmailAndContactNumber(String email, String contactNumber);
}

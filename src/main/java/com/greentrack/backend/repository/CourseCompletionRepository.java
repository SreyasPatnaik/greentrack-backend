package com.greentrack.backend.repository;

import com.greentrack.backend.model.CourseCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseCompletionRepository extends JpaRepository<CourseCompletion, Long> {
    List<CourseCompletion> findByUserId(Long userId);
    Optional<CourseCompletion> findByUserIdAndCourseId(Long userId, Integer courseId);
    void deleteByUserId(Long userId);
}

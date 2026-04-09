package com.greentrack.backend.repository;

import com.greentrack.backend.model.WasteReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WasteReportRepository extends JpaRepository<WasteReport, Long> {
    List<WasteReport> findByUserId(Long userId);
    List<WasteReport> findByStatusOrderByTimestampDesc(String status);
    List<WasteReport> findByUserIdOrderByTimestampDesc(Long userId);
    Long countByUserIdAndStatus(Long userId, String status);

    // Optimized: Fetch all approved report counts in a SINGLE query instead of N+1
    @Query("SELECT r.user.id, COUNT(r) FROM WasteReport r WHERE r.status = 'APPROVED' GROUP BY r.user.id")
    List<Object[]> countApprovedReportsGroupedByUser();

    // Count ALL reports (regardless of status) grouped by user — for leaderboard total contributions
    @Query("SELECT r.user.id, COUNT(r) FROM WasteReport r GROUP BY r.user.id")
    List<Object[]> countAllReportsGroupedByUser();
}

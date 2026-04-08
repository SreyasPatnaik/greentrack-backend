package com.greentrack.backend.repository;

import com.greentrack.backend.model.WasteReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WasteReportRepository extends JpaRepository<WasteReport, Long> {
    List<WasteReport> findByUserId(Long userId);
    List<WasteReport> findByStatusOrderByTimestampDesc(String status);
    List<WasteReport> findByUserIdOrderByTimestampDesc(Long userId);
}

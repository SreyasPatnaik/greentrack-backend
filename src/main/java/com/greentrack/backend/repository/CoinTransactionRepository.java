package com.greentrack.backend.repository;

import com.greentrack.backend.model.CoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {
    List<CoinTransaction> findByUserIdOrderByTimestampDesc(Long userId);
    List<CoinTransaction> findByUserIdAndReasonContaining(Long userId, String reasonFragment);
    void deleteAllByUserIdAndReasonContaining(Long userId, String reasonFragment);
}

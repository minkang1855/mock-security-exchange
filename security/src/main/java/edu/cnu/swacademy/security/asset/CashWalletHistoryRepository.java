package edu.cnu.swacademy.security.asset;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashWalletHistoryRepository extends JpaRepository<CashWalletHistory, Long> {

  List<CashWalletHistory> findByCashWalletId(Long cashWalletId);

  List<CashWalletHistory> findByCashWalletIdOrderByCreatedAtDesc(Long cashWalletId);
}

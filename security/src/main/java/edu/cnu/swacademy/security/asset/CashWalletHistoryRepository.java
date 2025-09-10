package edu.cnu.swacademy.security.asset;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashWalletHistoryRepository extends JpaRepository<CashWalletHistory, Integer> {

  List<CashWalletHistory> findByCashWalletId(int cashWalletId);

  List<CashWalletHistory> findByCashWalletIdOrderByCreatedAtDesc(int cashWalletId);
}

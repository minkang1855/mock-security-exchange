package edu.cnu.swacademy.security.asset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CashWalletHistoryRepository extends JpaRepository<CashWalletHistory, Integer> {

  @Query("SELECT cwh FROM CashWalletHistory cwh " +
         "JOIN cwh.cashWallet cw " +
         "WHERE cw.user.id = :userId " +
         "AND cw.isBlocked = false " +
         "AND cwh.txType IN :txTypes")
  Page<CashWalletHistory> findByUserIdAndTxTypeInAndWalletNotBlocked(
      int userId,
      List<TransactionType> txTypes,
      Pageable pageable
  );
}

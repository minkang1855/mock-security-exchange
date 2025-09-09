package edu.cnu.swacademy.security.asset;

import edu.cnu.swacademy.security.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE cash_wallet_history SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "cash_wallet_history")
@Entity
public class CashWalletHistory extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(columnDefinition = "INT UNSIGNED")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private CashWallet cashWallet;

  @Column(nullable = false, length = 20)
  private String txType;

  @Column(nullable = false)
  private long txAmount;

  @Column(nullable = false, length = 100)
  private String txNote;

  @Column(nullable = false, columnDefinition = "BIGINT UNSIGNED")
  private long reserve;
}

package edu.cnu.swacademy.security.asset;

import edu.cnu.swacademy.security.common.BaseEntity;
import edu.cnu.swacademy.security.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE cash_wallet SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "cash_wallet")
@Entity
public class CashWallet extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(columnDefinition = "INT UNSIGNED")
  private int id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private User user;

  @Column(nullable = false, length = 500, unique = true)
  private String accountNumber;

  @Column(nullable = false, columnDefinition = "BIGINT UNSIGNED DEFAULT 0")
  private int reserve = 0;

  @Column(nullable = false, columnDefinition = "BIGINT UNSIGNED DEFAULT 0")
  private int deposit = 0;

  @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
  private boolean isBlocked = false;

  public CashWallet(User user, String accountNumber) {
    this.user = user;
    this.accountNumber = accountNumber;
  }

  public void deposit(int amount) {
    this.reserve += amount;
  }

  public void withdrawal(int amount) {
    this.reserve -= amount;
  }
}

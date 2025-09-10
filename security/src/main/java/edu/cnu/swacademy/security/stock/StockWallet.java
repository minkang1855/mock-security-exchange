package edu.cnu.swacademy.security.stock;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import edu.cnu.swacademy.security.common.BaseEntity;
import edu.cnu.swacademy.security.user.User;
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

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE stock_wallet SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "stock_wallet")
@Entity
public class StockWallet extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(columnDefinition = "INT UNSIGNED")
  private int id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Stock stock;

  @Column(nullable = false, columnDefinition = "INT UNSIGNED DEFAULT 0")
  private int reserve;

  @Column(nullable = false, columnDefinition = "INT UNSIGNED DEFAULT 0")
  private int deposit;

  @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
  private boolean isBlocked;

  public StockWallet(User user, Stock stock) {
    this.user = user;
    this.stock = stock;
    this.reserve = 0;
    this.deposit = 0;
    this.isBlocked = false;
  }
}

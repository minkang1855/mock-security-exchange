package edu.cnu.swacademy.security.stock;

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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "stock_wallet")
@Entity
@SQLDelete(sql = "UPDATE stock_wallet SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class StockWallet extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(columnDefinition = "INT UNSIGNED")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Stock stock;

  @Column(nullable = false, columnDefinition = "INT UNSIGNED DEFAULT 0")
  private int reserve = 0;

  @Column(nullable = false, columnDefinition = "INT UNSIGNED DEFAULT 0")
  private int deposit = 0;

  @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
  private boolean isBlocked = false;
}

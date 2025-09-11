package edu.cnu.swacademy.security.order;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import edu.cnu.swacademy.security.common.BaseEntity;
import edu.cnu.swacademy.security.stock.Stock;
import edu.cnu.swacademy.security.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@SQLDelete(sql = "UPDATE `order` SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "order")
@Entity
public class Order extends BaseEntity {

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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderSide side;

  @Column(nullable = false, columnDefinition = "INT UNSIGNED")
  private int price;

  @Column(nullable = false, columnDefinition = "INT UNSIGNED")
  private int amount;

  @Column(nullable = false, columnDefinition = "INT UNSIGNED")
  private int unfilledAmount;

  @Column(nullable = false, columnDefinition = "INT UNSIGNED DEFAULT 0")
  private int canceledAmount;

  public Order(User user, Stock stock, OrderSide side, int price, int amount, int unfilledAmount) {
    this.user = user;
    this.stock = stock;
    this.side = side;
    this.price = price;
    this.amount = amount;
    this.unfilledAmount = unfilledAmount;
    this.canceledAmount = 0;
  }
}

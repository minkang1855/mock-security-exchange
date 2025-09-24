package edu.cnu.swacademy.security.market;

import edu.cnu.swacademy.security.common.BaseEntity;
import edu.cnu.swacademy.security.stock.Stock;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE market_status SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "market_status")
@Entity
public class MarketStatus extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(columnDefinition = "INT UNSIGNED")
  private int id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false)
  private Stock stock;

  @Column(nullable = false)
  private LocalDate tradingDate;

  @Column(nullable = false, columnDefinition = "INT UNSIGNED")
  private int referencePrice;

  @Column(nullable = false, columnDefinition = "INT UNSIGNED")
  private int upperLimitPrice;

  @Column(nullable = false, columnDefinition = "INT UNSIGNED")
  private int lowerLimitPrice;

  @Column(columnDefinition = "INT UNSIGNED")
  private int openingPrice;

  @Column(columnDefinition = "INT UNSIGNED")
  private int closingPrice;

  @Column(columnDefinition = "INT UNSIGNED")
  private int highestPrice;

  @Column(columnDefinition = "INT UNSIGNED")
  private int lowestPrice;

  @Column(columnDefinition = "INT UNSIGNED")
  private int tradingVolume; // 거래량

  @Column(columnDefinition = "BIGINT UNSIGNED")
  private long tradingAmount; // 거래 대금

  public MarketStatus(Stock stock, LocalDate tradingDate, int referencePrice, int upperLimitPrice, int lowerLimitPrice) {
    this.stock = stock;
    this.tradingDate = tradingDate;
    this.referencePrice = referencePrice;
    this.upperLimitPrice = upperLimitPrice;
    this.lowerLimitPrice = lowerLimitPrice;
    this.openingPrice = 0;
    this.closingPrice = 0;
    this.highestPrice = 0;
    this.lowestPrice = 0;
    this.tradingVolume = 0;
    this.tradingAmount = 0;
  }

  public void update(int price, int quantity, int tradingAmount) {
    this.openingPrice = this.openingPrice == 0 ? price : this.openingPrice;
    this.lowestPrice = (this.lowestPrice == 0 || price < this.lowestPrice) ? price : this.lowestPrice;
    this.highestPrice = (this.highestPrice == 0 || this.highestPrice < price) ? price : this.highestPrice;
    this.tradingVolume += quantity;
    this.tradingAmount += tradingAmount;
  }
}

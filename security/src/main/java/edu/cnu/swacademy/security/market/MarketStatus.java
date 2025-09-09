package edu.cnu.swacademy.security.market;

import java.time.LocalDate;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "market_status")
@Entity
@SQLDelete(sql = "UPDATE market_status SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class MarketStatus extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(columnDefinition = "INT UNSIGNED")
  private Long id;

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
  private int tradingVolume;

  @Column(columnDefinition = "BIGINT UNSIGNED")
  private long tradingAmount;
}

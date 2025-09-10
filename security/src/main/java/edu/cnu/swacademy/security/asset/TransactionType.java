package edu.cnu.swacademy.security.asset;

import lombok.Getter;

@Getter
public enum TransactionType {
  DEPOSIT("입금"),
  WITHDRAWAL("출금"),
  TRADE_PAYMENT("거래 대금 지급(거래 출금)"),
  TRADE_RECEIPT("거래 대금 수령(거래 입금)"),
  TRADE_REFUND("거래 대금 반환(주문 취소)"),
  ACCOUNT_BLOCKED("계좌 정지"),
  ACCOUNT_UNBLOCKED("계좌 정지 해제");

  private final String description;

  TransactionType(String description) {
    this.description = description;
  }
}

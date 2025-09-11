package edu.cnu.swacademy.security.stock;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockWalletTransactionType {
  
  DEPOSIT("입고"),
  SELL_ORDER("판매 주문 (매도 수량 설정)"),
  SELL_ORDER_CANCEL("판매 주문 취소 (매도 수량 해제)"),
  BUY_ORDER_EXECUTED("구매 주문 체결"),
  SELL_ORDER_EXECUTED("판매 주문 체결"),
  ACCOUNT_BLOCKED("계좌 정지"),
  ACCOUNT_UNBLOCKED("계좌 정지 해제");

  private final String description;
}

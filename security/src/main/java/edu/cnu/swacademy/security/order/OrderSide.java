package edu.cnu.swacademy.security.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 주문 방향 Enum
 */
@Getter
@RequiredArgsConstructor
public enum OrderSide {
    BUY("BUY"),
    SELL("SELL");
    
    private final String value;
    
    @Override
    public String toString() {
        return value;
    }
}

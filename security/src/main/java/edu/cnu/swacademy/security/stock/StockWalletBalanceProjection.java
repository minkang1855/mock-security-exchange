package edu.cnu.swacademy.security.stock;

/**
 * 종목 지갑 잔고 조회용 Projection 클래스
 */
public class StockWalletBalanceProjection {
    private int id;
    private int reserve;
    private int deposit;
    private boolean isBlocked;
    
    public StockWalletBalanceProjection(int id, int reserve, int deposit, boolean isBlocked) {
        this.id = id;
        this.reserve = reserve;
        this.deposit = deposit;
        this.isBlocked = isBlocked;
    }
    
    public int getId() {
        return id;
    }
    
    public int getReserve() {
        return reserve;
    }
    
    public int getDeposit() {
        return deposit;
    }
    
    public boolean isBlocked() {
        return isBlocked;
    }
    
    public int getAvailable() {
        return reserve - deposit;
    }
}
package edu.cnu.swacademy.security.stock;

import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.stock.dto.StockBalanceResponse;
import edu.cnu.swacademy.security.stock.dto.StockDepositRequest;
import edu.cnu.swacademy.security.stock.dto.StockWalletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/stock-wallet")
@RestController
public class StockWalletController {

  private final StockWalletService stockWalletService;

  @PostMapping
  public void createStockWallet(
      HttpServletRequest request,
      @Valid @RequestBody StockWalletRequest stockWalletRequest
  ) throws SecurityException {
    int userId = (int) request.getAttribute("user_id");
    stockWalletService.createStockWallet(userId, stockWalletRequest);
  }

  @PostMapping("/{stock_id}/deposit")
  public void deposit(
      HttpServletRequest request,
      @PathVariable("stock_id") int stockId,
      @Valid @RequestBody StockDepositRequest depositRequest
  ) throws SecurityException {
    int userId = (int) request.getAttribute("user_id");
    stockWalletService.deposit(userId, stockId, depositRequest);
  }

  @GetMapping("/{stock_id}/balance")
  public StockBalanceResponse getBalance(
      HttpServletRequest request,
      @PathVariable("stock_id") int stockId
  ) throws SecurityException {
    int userId = (int) request.getAttribute("user_id");
    return stockWalletService.getBalance(userId, stockId);
  }

  @PostMapping("/{stock_wallet_id}/block")
  public void blockStockWallet(
      @PathVariable("stock_wallet_id") int stockWalletId
  ) throws SecurityException {
    stockWalletService.blockStockWallet(stockWalletId);
  }

  @PostMapping("/{stock_wallet_id}/unblock")
  public void unblockStockWallet(
      @PathVariable("stock_wallet_id") int stockWalletId
  ) throws SecurityException {
    stockWalletService.unblockStockWallet(stockWalletId);
  }
}

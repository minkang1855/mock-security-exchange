package edu.cnu.swacademy.security.stock;

import edu.cnu.swacademy.security.stock.dto.StockBalanceResponse;
import edu.cnu.swacademy.security.stock.dto.StockWalletRequest;
import edu.cnu.swacademy.security.common.SecurityException;
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

  @GetMapping("/{stockId}/balance")
  public StockBalanceResponse getBalance(
      HttpServletRequest request,
      @PathVariable int stockId
  ) throws SecurityException {
    int userId = (int) request.getAttribute("user_id");
    return stockWalletService.getBalance(userId, stockId);
  }
}

package edu.cnu.swacademy.security.asset;

import edu.cnu.swacademy.security.asset.dto.CashDepositRequest;
import edu.cnu.swacademy.security.asset.dto.CashWithdrawalRequest;
import edu.cnu.swacademy.security.common.SecurityException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/api/v1/cash-wallet")
@RestController
public class CashWalletController {

  private final CashWalletService cashWalletService;

  @PostMapping
  public void createCashWallet(HttpServletRequest request) throws SecurityException {
    Long userId = (Long) request.getAttribute("user_id");
    cashWalletService.createCashWallet(userId);
  }

  @PostMapping("/deposit")
  public void deposit(HttpServletRequest request, @Valid @RequestBody CashDepositRequest depositRequest) throws SecurityException {
    Long userId = (Long) request.getAttribute("user_id");
    cashWalletService.deposit(userId, depositRequest);
  }

  @PostMapping("/withdrawal")
  public void withdrawal(HttpServletRequest request, @Valid @RequestBody CashWithdrawalRequest withdrawalRequest) throws SecurityException {
    Long userId = (Long) request.getAttribute("user_id");
    cashWalletService.withdrawal(userId, withdrawalRequest);
  }
}

package edu.cnu.swacademy.security.asset;

import edu.cnu.swacademy.security.common.SecurityException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
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
}

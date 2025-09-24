package edu.cnu.swacademy.security.stock;

import edu.cnu.swacademy.security.common.ErrorCode;
import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.stock.dto.StockBalanceResponse;
import edu.cnu.swacademy.security.stock.dto.StockDepositRequest;
import edu.cnu.swacademy.security.stock.dto.StockWalletRequest;
import edu.cnu.swacademy.security.user.User;
import edu.cnu.swacademy.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class StockWalletService {

  private final StockWalletRepository stockWalletRepository;
  private final StockWalletHistoryRepository stockWalletHistoryRepository;
  private final StockRepository stockRepository;
  private final UserRepository userRepository;

  /**
   * 증권 계좌 개설
   * 사용자의 종목 계좌를 생성합니다.
   * 
   * @param userId 사용자 ID (JWT에서 추출된 값)
   * @param request 증권 계좌 개설 요청 (stockId 포함)
   * @throws SecurityException 개설 실패 시 발생 (종목 없음, 이미 존재함)
   */
  @Transactional(rollbackFor = Exception.class)
  public void createStockWallet(int userId, StockWalletRequest request) throws SecurityException {
    // 1. 사용자 존재 여부 확인
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.info("User not found for user-id(={})", userId);
          return new SecurityException(ErrorCode.USER_NOT_FOUND);
        });

    // 2. 종목 존재 여부 확인
    Stock stock = stockRepository.findById(request.stockId())
        .orElseThrow(() -> {
          log.info("Stock not found for stock-id(={})", request.stockId());
          return new SecurityException(ErrorCode.STOCK_NOT_FOUND);
        });

    // 3. 이미 해당 종목에 대한 계좌가 존재하는지 확인
    if (stockWalletRepository.findByUserIdAndStockIdWithLock(userId, request.stockId()).isPresent()) {
      log.info("Stock wallet already exists for user-id(={}), stock-id(={})", userId, request.stockId());
      throw new SecurityException(ErrorCode.STOCK_WALLET_ALREADY_EXISTS);
    }

    // 4. 증권 계좌 생성
    StockWallet stockWallet = new StockWallet(user, stock);
    stockWalletRepository.save(stockWallet);

    log.info("Stock wallet created successfully: user-id(={}), stock-id(={}), stock-wallet-id(={})",
        userId, request.stockId(), stockWallet.getId());
  }

  /**
   * 증권 입고
   * 입고 수량만큼 사용자의 증권 계좌의 보유 잔고를 증가시킵니다.
   *
   * @param request 증권 입고 요청 (종목 계좌 ID, amount 포함)
   * @throws SecurityException 입고 실패 시 발생 (종목 계좌 없음, 정지됨)
   */
  @Transactional(rollbackFor = Exception.class)
  public void deposit(StockDepositRequest request) throws SecurityException {
    // 1. 사용자의 특정 종목 증권 계좌 조회
    StockWallet stockWallet = stockWalletRepository.findById(request.stockWalletId())
        .orElseThrow(() -> {
          log.info("Stock wallet not found for id(={})", request.stockWalletId());
          return new SecurityException(ErrorCode.STOCK_WALLET_NOT_FOUND);
        });

    // 2. 종목 계좌 정지 상태 확인
    if (stockWallet.isBlocked()) {
      log.info("Stock wallet is blocked for id(={})", stockWallet.getId());
      throw new SecurityException(ErrorCode.STOCK_WALLET_BLOCKED);
    }

    // 3. 증권 입고 처리
    stockWallet.deposit(request.amount());

    stockWalletHistoryRepository.save(
        new StockWalletHistory(
            stockWallet,
            StockWalletTransactionType.DEPOSIT,
            request.amount(),
            "증권 입고",
            stockWallet.getReserve()
        )
    );

    log.info("Stock deposit completed: stock-wallet-id(={})", stockWallet.getId());
  }

  /**
   * 증권 계좌 잔고 조회
   * 사용자가 보유한 특정 종목의 증권계좌 상태를 확인하여, 실제 보유하고 있는 잔고(reserve),
   * 매도 주문에 묶여 있는 증거량(deposit), 그리고 매도 가능 잔고(available = reserve - deposit)를 조회합니다.
   * 
   * @param userId 사용자 ID (JWT에서 추출된 값)
   * @param stockId 종목 ID
   * @return 증권 계좌 잔고 정보
   * @throws SecurityException 조회 실패 시 발생 (종목 계좌 없음)
   */
  public StockBalanceResponse getBalance(int userId, int stockId) throws SecurityException {
    // 1. 사용자의 특정 종목 증권 계좌 조회
    StockWallet stockWallet = stockWalletRepository.findByUserIdAndStockId(userId, stockId)
        .orElseThrow(() -> {
          log.info("Stock wallet not found for user-id(={}), stock-id(={})", userId, stockId);
          return new SecurityException(ErrorCode.STOCK_WALLET_NOT_FOUND);
        });

    if (stockWallet.isBlocked()) {
      throw new SecurityException(ErrorCode.STOCK_WALLET_BLOCKED);
    }

    // 2. 잔고 계산
    int reserve = stockWallet.getReserve();
    int deposit = stockWallet.getDeposit();
    int available = reserve - deposit;

    log.info("Stock wallet balance retrieved: stock-wallet-id(={})", stockWallet.getId());

    return new StockBalanceResponse(
        stockWallet.getId(),
        stockId,
        reserve,
        deposit,
        available
    );
  }

  /**
   * 종목 계좌 정지
   * 사용자의 종목 계좌을 정지하여 모든 종목 관련 거래를 차단합니다.
   *
   * @param stockWalletId 종목 계좌 ID (PathVariable로 받은 값)
   * @throws SecurityException 정지 실패 시 발생 (종목 계좌 없음, 이미 정지됨)
   */
  @Transactional(rollbackFor = Exception.class)
  public void blockStockWallet(int stockWalletId) throws SecurityException {
    // 1. 사용자의 종목 계좌 조회
    StockWallet stockWallet = stockWalletRepository.findById(stockWalletId)
        .orElseThrow(() -> {
          log.info("Stock wallet not found for id(={})", stockWalletId);
          return new SecurityException(ErrorCode.STOCK_WALLET_NOT_FOUND);
        });

    // 2. 이미 정지된 계좌인지 확인
    if (stockWallet.isBlocked()) {
      log.info("Stock wallet is already blocked for id(={})", stockWalletId);
      throw new SecurityException(ErrorCode.STOCK_WALLET_ALREADY_BLOCKED);
    }

    // 3. 종목 계좌 정지 처리
    stockWallet.block();

    // 4. 계좌 정지 내역 생성 (변경 내역 기록)

    StockWalletHistory history = new StockWalletHistory(
        stockWallet,
        StockWalletTransactionType.ACCOUNT_BLOCKED,
        0, // 정지 시에는 금액 변동 없음
        "계좌 정지",
        stockWallet.getReserve()
    );
    stockWalletHistoryRepository.save(history);

    log.info("Stock wallet blocked successfully: stock-wallet-id(={})", stockWallet.getId());
  }

  /**
   * 종목 계좌 정지 해제
   * 정지 상태로 잠겨 있던 종목 계좌를 정상 상태로 되돌려, 다시 매도 주문 접수가 가능하도록 허용합니다.
   *
   * @param stockWalletId 종목 계좌 ID
   * @throws SecurityException 해제 실패 시 발생 (종목 계좌 없음, 이미 해제됨)
   */
  @Transactional(rollbackFor = Exception.class)
  public void unblockStockWallet(int stockWalletId) throws SecurityException {
    // 1. 현금 계좌 조회
    StockWallet stockWallet = stockWalletRepository.findById(stockWalletId)
        .orElseThrow(() -> {
          log.info("Stock wallet not found for id(={})", stockWalletId);
          return new SecurityException(ErrorCode.STOCK_WALLET_NOT_FOUND);
        });

    // 2. 이미 정지 해제된 계좌인지 확인
    if (!stockWallet.isBlocked()) {
      log.info("Stock wallet is already unblocked for id(={})", stockWallet.getId());
      throw new SecurityException(ErrorCode.STOCK_WALLET_ALREADY_UNBLOCKED);
    }

    // 3. 계좌 정지 해제 처리
    stockWallet.unblock();

    // 4. 계좌 정지 해제 내역 생성 (변경 내역 기록)
    StockWalletHistory history = new StockWalletHistory(
        stockWallet,
        StockWalletTransactionType.ACCOUNT_UNBLOCKED,
        0, // 해제 시에는 금액 변동 없음
        "계좌 정지 해제",
        stockWallet.getReserve()
    );
    stockWalletHistoryRepository.save(history);

    log.info("Cash wallet unblocked successfully: cash-wallet-id(={})", stockWallet.getId());
  }
}

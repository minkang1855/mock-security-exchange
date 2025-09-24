package edu.cnu.swacademy.security.orderbook;

import edu.cnu.swacademy.security.common.ErrorCode;
import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.orderbook.dto.OrderBookResponse;
import edu.cnu.swacademy.security.stock.StockRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 오더북 조회 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/orderbook")
@RequiredArgsConstructor
@Slf4j
public class OrderBookController {

    private final OrderBookService orderBookService;
    private final StockRepository stockRepository;

    /**
     * 오더북 조회
     * GET /api/v1/orderbook/{stock_id}
     */
    @GetMapping("/{stockId}")
    public OrderBookResponse getOrderBook(
        HttpServletRequest request,
        @PathVariable int stockId
    ) throws SecurityException {
        // JWT 인증 확인 (user_id 추출)
        int userId = (int) request.getAttribute("user_id");
        log.info("User {} requesting order book for stockId: {}", userId, stockId);

        // 1. 종목 존재 여부 확인
        if (!stockRepository.existsById(stockId)) {
            log.warn("Stock not found: stockId={}", stockId);
            throw new SecurityException(ErrorCode.STOCK_NOT_FOUND);
        }

        // 2. 오더북 조회
        OrderBookResponse orderBook = orderBookService.getOrderBook(stockId);
        
        log.info("Order book retrieved successfully: stockId={}, buy prices: {}, sell prices: {}", 
            stockId, orderBook.buy().prices().size(), orderBook.sell().prices().size());
        return orderBook;
    }
}

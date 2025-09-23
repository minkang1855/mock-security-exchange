import os
import time
import requests

# ===== 설정 =====
BASE_URL = os.getenv("BASE_URL", "http://localhost:8080").rstrip("/")
DEPOSIT_PATH = "/api/v1/stock-wallet/reserve"
TIMEOUT = 5
RETRY = 2          # 5xx/네트워크 오류 재시도 횟수
TOTAL_USERS = 10_000  # 총 사용자 수 (stock_wallet_id 1~10,000)
DEPOSIT_AMOUNT = 500  # 입고 수량

def make_request(method: str, url: str, headers: dict = None, json_data: dict = None, timeout: int = TIMEOUT):
    for attempt in range(RETRY + 1):
        try:
            if method.upper() == "GET":
                resp = requests.get(url, headers=headers, timeout=timeout)
            elif method.upper() == "POST":
                resp = requests.post(url, headers=headers, json=json_data, timeout=timeout)
            elif method.upper() == "PUT":
                resp = requests.put(url, headers=headers, json=json_data, timeout=timeout)
            elif method.upper() == "DELETE":
                resp = requests.delete(url, headers=headers, timeout=timeout)
            else:
                return False, -1, f"Unsupported method: {method}"

            # 성공: 2xx
            if 200 <= resp.status_code < 300:
                return True, resp.status_code, None

            # 클라이언트 오류(4xx)는 재시도 무의미
            if 400 <= resp.status_code < 500:
                return False, resp.status_code, resp.text.strip() or "CLIENT_ERROR"

            # 서버 오류(5xx)는 재시도
            if 500 <= resp.status_code < 600 and attempt < RETRY:
                time.sleep(0.5 * (attempt + 1))
                continue

            # 기타 코드
            return False, resp.status_code, resp.text.strip() or "UNEXPECTED_STATUS"

        except (requests.exceptions.ConnectionError, requests.exceptions.Timeout) as e:
            if attempt < RETRY:
                time.sleep(0.5 * (attempt + 1))
                continue
            return False, -1, f"NETWORK_ERROR: {e.__class__.__name__}"

    return False, -1, "UNKNOWN_ERROR"

def deposit_stock_wallet(stock_wallet_id: int, amount: int):
    url = f"{BASE_URL}{DEPOSIT_PATH}"
    json_data = {
        "stock_wallet_id": stock_wallet_id,
        "amount": amount
    }
    return make_request("POST", url, json_data=json_data)


if __name__ == "__main__":
    success_count = 0
    fail_count = 0

    for stock_wallet_id in range(1, TOTAL_USERS + 1):
        ok, code, note = deposit_stock_wallet(stock_wallet_id, DEPOSIT_AMOUNT)

        if ok:
            success_count += 1
        else:
            fail_count += 1

        # 진행 로그(1000명 마다)
        if stock_wallet_id % 1000 == 0:
            print(f"[INFO] Processed {stock_wallet_id}/{TOTAL_USERS} wallets (Success: {success_count}, Failed: {fail_count})")

    print("-" * 50)
    print(f"Successful deposits: {success_count}")
    print(f"Failed deposits: {fail_count}")
    print(f"Success rate: {(success_count/TOTAL_USERS*100):.1f}%")
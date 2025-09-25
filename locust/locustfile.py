import csv
import random
import time
from locust import HttpUser, task, between, events

# 전역 설정
BASE_URL = "http://localhost:8080"
STOCK_ID = 1  # 모든 주문에서 stock_id 고정
MIN_PRICE = 66500
MAX_PRICE = 73500
TICK_SIZE = 100  # 100원 단위 틱 사이즈
INPUT_CSV = "logins_out.csv"

# 사용자 데이터 로드
users_data = []

def load_logins(csv_path):
    """logins_out.csv에서 사용자 데이터를 로드합니다."""
    users = []
    try:
        with open(csv_path, newline="", encoding="utf-8") as f:
            reader = csv.DictReader(f, delimiter=':')  # 콜론 구분자 추가
            for row in reader:
                uid = (row.get("user_id") or "").strip()
                email = (row.get("user_email") or "").strip()
                at = (row.get("access_token") or "").strip()
                rt = (row.get("refresh_token") or "").strip()
                if uid and email and at:
                    users.append({
                        "user_id": uid,
                        "user_email": email,
                        "access_token": at,
                        "refresh_token": rt
                    })
        print(f"Loaded {len(users)} users from {csv_path}")
    except FileNotFoundError:
        print(f"{csv_path} file not found. Please ensure the file exists.")
    except Exception as e:
        print(f"Error loading users: {e}")
    return users

def generate_tick_price():
    """틱 사이즈에 맞는 가격을 생성합니다."""
    base_price = random.randint(MIN_PRICE, MAX_PRICE)
    # 100원 단위로 조정
    tick_price = (base_price // TICK_SIZE) * TICK_SIZE
    return tick_price

def auth_headers(access_token):
    return {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }

class SecurityExchangeUser(HttpUser):
    @events.test_start.add_listener
    def on_test_start(environment, **kwargs):
        global users_data
        users_data = load_logins(INPUT_CSV)
        return

    wait_time = between(1, 3)

    def on_start(self):
        if not users_data:
            print("No user data available. Please check logins_out.csv file.")
            return

        # 랜덤 사용자 선택
        user_info = random.choice(users_data)
        self.user_id = user_info['user_id']
        self.email = user_info['user_email']
        self.access_token = user_info['access_token']
        self.refresh_token = user_info['refresh_token']

        print(f"User {self.user_id} started session")

    def get_authenticated(self, url: str):
        """인증이 필요한 API 호출"""
        header = auth_headers(self.access_token)
        return self.client.get(url, headers=header)

    def post_authenticated(self, url: str, json_data: dict):
        """인증이 필요한 POST API 호출"""
        header = auth_headers(self.access_token)
        return self.client.post(url, headers=header, json=json_data)

    def delete_authenticated(self, url: str):
        """인증이 필요한 DELETE API 호출"""
        header = auth_headers(self.access_token)
        return self.client.delete(url, headers=header)

    # 현금 입금 시나리오 묶음
    @task
    def cashDepositScenario(self):
        # 현금 입금
        amount = random.randint(100000, 1000000)
        self.post_authenticated("/api/v1/cash-wallet/deposit", {"amount": amount})
        # 현금 잔액 조회
        self.get_authenticated("/api/v1/cash-wallet/balance")
        # 입출금 내역 조회
        self.get_authenticated("/api/v1/cash-wallet/histories")

    # 현금 출금 시나리오 묶음
    @task
    def cashWithdrawalScenario(self):
        # 현금 출금
        amount = random.randint(50000, 500000)
        self.post_authenticated("/api/v1/cash-wallet/withdrawal", {"amount": amount})
        # 현금 잔액 조회
        self.get_authenticated("/api/v1/cash-wallet/balance")
        # 입출금 내역 조회
        self.get_authenticated("/api/v1/cash-wallet/histories")

    # 미체결 주문 추가 시나리오 묶음
    @task
    def unfilledOrderScenario(self):
        # 현재 잔고 조회
        self.get_authenticated(f"/api/v1/stock-wallet/balance/{STOCK_ID}")
        # 오더북 조회
        self.get_authenticated(f"/api/v1/orderbook/{STOCK_ID}")
        # 주문 접수
        price = generate_tick_price()
        quantity = random.randint(1, 10)
        side = random.choice(["buy", "sell"])
        self.post_authenticated("/api/v1/order", {"stock_id": STOCK_ID, "side": side, "price": price, "quantity": quantity})
        # 당일 미체결 주문 내역 조회
        self.get_authenticated("/api/v1/order/unfilled")

    # 주문 체결 시나리오 묶음 (모든 주문이 체결되도록)
    @task
    def orderExecutionScenario(self):
        # 현재 잔고 조회
        self.get_authenticated(f"/api/v1/stock-wallet/balance/{STOCK_ID}")
        # 오더북 조회
        self.get_authenticated(f"/api/v1/orderbook/{STOCK_ID}")
        # 첫 번째 주문 접수 (매수)
        price = generate_tick_price()
        quantity = random.randint(1, 5)
        self.post_authenticated("/api/v1/order", {"stock_id": STOCK_ID, "side": "buy", "price": price, "quantity": quantity})
        # 두 번째 주문 접수 (매도 - 같은 가격으로 체결 유도)
        self.post_authenticated("/api/v1/order", {"stock_id": STOCK_ID, "side": "sell", "price": price, "quantity": quantity})
        # 당일 미체결 주문 내역 조회
        self.get_authenticated("/api/v1/order/unfilled")
        # 체결 내역 조회
        self.get_authenticated("/api/v1/match")

    # 주문 부분 체결 시나리오 묶음 (부분만 체결되도록)
    @task
    def partialExecutionScenario(self):
        # 현재 잔고 조회
        self.get_authenticated(f"/api/v1/stock-wallet/balance/{STOCK_ID}")
        # 오더북 조회
        self.get_authenticated(f"/api/v1/orderbook/{STOCK_ID}")
        # 첫 번째 주문 접수 (큰 수량)
        price = generate_tick_price()
        large_quantity = random.randint(10, 20)
        self.post_authenticated("/api/v1/order", {"stock_id": STOCK_ID, "side": "buy", "price": price, "quantity": large_quantity})
        # 두 번째 주문 접수 (작은 수량 - 부분 체결 유도)
        small_quantity = random.randint(1, 3)
        self.post_authenticated("/api/v1/order", {"stock_id": STOCK_ID, "side": "sell", "price": price, "quantity": small_quantity})
        # 당일 미체결 주문 내역 조회
        self.get_authenticated("/api/v1/order/unfilled")
        # 체결 내역 조회
        self.get_authenticated("/api/v1/match")

    # 주문 취소 시나리오 묶음
    @task
    def orderCancelScenario(self):
        # 현재 잔고 조회
        self.get_authenticated(f"/api/v1/stock-wallet/balance/{STOCK_ID}")
        # 당일 미체결 주문 내역 조회
        self.get_authenticated("/api/v1/order/unfilled")
        # 주문 접수 (취소용)
        price = generate_tick_price()
        quantity = random.randint(1, 5)
        side = random.choice(["buy", "sell"])
        self.post_authenticated("/api/v1/order", {"stock_id": STOCK_ID, "side": side, "price": price, "quantity": quantity})
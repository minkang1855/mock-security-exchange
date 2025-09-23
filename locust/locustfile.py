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

    # 개별 API 호출들 (높은 빈도)

    # 현금 잔액 조회
    @task
    def cashBalance(self):
        self.get_authenticated("/api/v1/cash-wallet/balance")

    # 입출금 내역 조회
    @task
    def cashHistories(self):
        self.get_authenticated("/api/v1/cash-wallet/histories")

    # 현재 잔고 조회
    @task
    def stockBalance(self):
        self.get_authenticated(f"/api/v1/stock-wallet/balance/{STOCK_ID}")

    # 당일 미체결 주문 내역 조회
    @task
    def unfilledOrders(self):
        self.get_authenticated("/api/v1/orders/unfilled")

    # 체결 내역 조회
    @task
    def matchHistory(self):
        self.get_authenticated("/api/v1/match")

    # 현금 입금 (단일)
    @task
    def cashDeposit(self):
        amount = random.randint(100000, 1000000)
        self.post_authenticated("/api/v1/cash-wallet/deposit", {"amount": amount})

    # 현금 출금 (단일)
    @task
    def cashWithdrawal(self):
        amount = random.randint(50000, 500000)
        self.post_authenticated("/api/v1/cash-wallet/withdrawal", {"amount": amount})
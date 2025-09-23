import os
import csv
import time
import requests

# ===== 설정 =====
BASE_URL = os.getenv("BASE_URL", "http://localhost:8080").rstrip("/")
OPEN_WALLET_PATH = "/api/v1/cash-wallet"
INPUT_CSV = "logins_out.csv"         # 입력: user_email:user_id:access_token:refresh_token (콜론 구분)
TIMEOUT = 5
RETRY = 2          # 5xx/네트워크 오류 재시도 횟수

def load_logins(csv_path):
    users = []
    with open(csv_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f, delimiter=':')  # 콜론 구분자
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
    return users

def create_cash_wallet(access_token: str):
    url = f"{BASE_URL}{OPEN_WALLET_PATH}"
    headers = {"Authorization": f"Bearer {access_token}"}

    for attempt in range(RETRY + 1):
        try:
            resp = requests.post(url, headers=headers, timeout=TIMEOUT)

            # 성공: 200
            if resp.status_code == 200:
                return True, resp.status_code, None

            # 이미 개설 (409)
            if resp.status_code == 409:
                return True, resp.status_code, "ALREADY_EXISTS"

            # 인증 실패(401), 유효성 오류(400) 등은 재시도 무의미
            if resp.status_code in (400, 401, 404):
                return False, resp.status_code, resp.text.strip() or "CLIENT_ERROR"

            # 그 외(5xx) 재시도
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

if __name__ == "__main__":
    users = load_logins(INPUT_CSV)
    if not users:
        print("No users loaded. Exiting.")
        exit(1)
    
    success_count = 0
    fail_count = 0
    
    for i, u in enumerate(users, start=1):
        user_id = u["user_id"]
        user_email = u["user_email"]
        access_token = u["access_token"]
        
        ok, code, note = create_cash_wallet(access_token)
        
        if ok:
            success_count += 1
        else:
            fail_count += 1
        
        # 진행 로그(1000명 마다)
        if i % 1000 == 0:
            print(f"[INFO] Processed {i}/{len(users)} users (Success: {success_count}, Failed: {fail_count})")

    print("-" * 50)
    print(f"Successful creations: {success_count}")
    print(f"Failed creations: {fail_count}")
    print(f"Success rate: {(success_count / len(users) * 100):.1f}%")
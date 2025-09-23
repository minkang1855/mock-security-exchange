import os
import csv
import json
import time
import base64
import hashlib
import requests
from typing import List, Dict

# ===== 설정 =====
BASE_URL = os.getenv("BASE_URL", "http://localhost:8080").rstrip("/")
LOGIN_PATH = "/api/v1/auth/login"
INPUT_CSV = "users.csv"
OUTPUT_CSV = "logins_out.csv"
HARDCODED_PASSWORD = "Password1234!"
TIMEOUT = 5
RETRY = 2

def sha512_base64(input_string: str) -> str:
    try:
        # SHA-512 해시 계산
        hash_bytes = hashlib.sha512(input_string.encode('utf-8')).digest()
        # Base64로 인코딩
        return base64.b64encode(hash_bytes).decode('utf-8')
    except Exception as e:
        print(f"SHA-512 hashing error: {e}")
        return ""

def b64url_decode(data: str) -> bytes:
    padding = '=' * (-len(data) % 4)
    return base64.urlsafe_b64decode(data + padding)

def decode_jwt_sub(jwt_token: str) -> str:
    try:
        parts = jwt_token.split(".")
        if len(parts) != 3:
            return ""
        payload_b = b64url_decode(parts[1])
        payload = json.loads(payload_b.decode("utf-8"))
        sub = payload.get("sub")
        return str(sub) if sub is not None else ""
    except Exception:
        return ""

def login(email: str, password: str) -> Dict[str, str]:
    url = f"{BASE_URL}{LOGIN_PATH}"
    body = {"user_email": email, "user_password": HARDCODED_PASSWORD}

    for attempt in range(RETRY + 1):
        try:
            resp = requests.post(url, json=body, timeout=TIMEOUT)
            
            if resp.status_code == 200:
                data = resp.json()
                access_token = data.get("access_token", "")
                refresh_token = data.get("refresh_token", "")
                user_id = decode_jwt_sub(access_token) if access_token else ""
                return {
                    "user_email": email,
                    "user_id": user_id,
                    "access_token": access_token,
                    "refresh_token": refresh_token,
                }
            elif resp.status_code in (401, 404):
                return {}
            else:
                if attempt < RETRY:
                    time.sleep(0.5 * (attempt + 1))
                    continue
                return {}
        except (requests.exceptions.ConnectionError, requests.exceptions.Timeout):
            if attempt < RETRY:
                time.sleep(0.5 * (attempt + 1))
                continue
            return {}
    return {}

def load_users(csv_path: str) -> List[Dict[str, str]]:
    users = []
    try:
        with open(csv_path, newline="", encoding="utf-8") as f:
            reader = csv.DictReader(f, delimiter=':')
            for row in reader:
                name = (row.get("name") or "").strip()
                email = (row.get("email") or "").strip()
                
                if not email:
                    continue
                    
                users.append({
                    "name": name,
                    "user_email": email, 
                    "user_password": HARDCODED_PASSWORD
                })
    except FileNotFoundError:
        print(f"Error: {csv_path} file not found!")
    except Exception as e:
        print(f"Error loading users: {e}")
    return users

def save_results(csv_path: str, rows: List[Dict[str, str]]):
    fields = ["user_email", "user_id", "access_token", "refresh_token"]
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fields, delimiter=':')
        writer.writeheader()
        for r in rows:
            writer.writerow({
                "user_email": r.get("user_email", ""),
                "user_id": r.get("user_id", ""),
                "access_token": r.get("access_token", ""),
                "refresh_token": r.get("refresh_token", ""),
            })

if __name__ == "__main__":
    users = load_users(INPUT_CSV)
    if not users:
        print("No users loaded. Exiting.")
        exit(1)
    
    results = []
    success_count = 0
    
    for i, u in enumerate(users, 1):
        res = login(u["user_email"], u["user_password"])
        if res:
            results.append(res)
            success_count += 1
        
        # 1000 단위로 성공 로그 출력
        if i % 1000 == 0:
            print(f"[SUCCESS LOG] Processed {i}/{len(users)} users - Success: {success_count}")

    save_results(OUTPUT_CSV, results)
    print(f"[DONE] Processed {len(users)} users, {len(results)} successful logins")
    print(f"Results saved to: {OUTPUT_CSV}")
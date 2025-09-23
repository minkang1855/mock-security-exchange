import base64
import random
import hashlib

# 영문 이름 리스트 (10글자 제한을 고려한 짧은 이름들)
first_names = ['John', 'Jane', 'Mike', 'Sara', 'Dave', 'Lisa', 'Rob', 'Amy', 'Jim', 'Jess', 'Will', 'Ash', 'Rick', 'Ann', 'Tom', 'Kim', 'Chris', 'Kate', 'Alex', 'Sam']
last_names = ['Kim', 'Lee', 'Park', 'Choi', 'Jung', 'Kang', 'Cho', 'Yoon', 'Jang', 'Lim', 'Han', 'Oh', 'Seo', 'Shin', 'Kwon', 'Hwang', 'Ahn', 'Song', 'Jeon', 'Ko']

def sha512_hash(password):
    hash_bytes = hashlib.sha512(password.encode('utf-8')).digest()
    return base64.b64encode(hash_bytes).decode('utf-8')

def generate_name():
    while True:
        first_name = random.choice(first_names)
        last_name = random.choice(last_names)
        full_name = f"{first_name} {last_name}"

        # 공백 포함해서 10글자 이하인지 확인
        if len(full_name) <= 10:
            return full_name

if __name__ == "__main__":
    # 고정 비밀번호
    plain_password = "Password1234!"
    hashed_password = sha512_hash(plain_password)

    # CSV 파일 생성 (ASCII 인코딩)
    with open('users.csv', 'w', encoding='utf-8') as file:
        # 헤더 작성
        file.write("name:email:password\n")

        for i in range(1, 10001):
            # 10글자 제한으로 이름 생성
            name = generate_name()

            # 이메일 생성 (순차적)
            email = f"user{i:05d}@example.com"

            # 행 작성
            file.write(f"{name}:{email}:{hashed_password}\n")
    print("users.csv 파일 생성")

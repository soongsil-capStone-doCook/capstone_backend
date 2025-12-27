# 1. 가볍고 안정적인 Python 3.10 Slim 버전 사용
FROM python:3.10-slim

# 2. 컨테이너 내 작업 디렉토리 설정
WORKDIR /app

# 3. 필수 시스템 패키지 설치 (빌드 도구 등)
# (나중에 opencv 같은 라이브러리가 필요하면 여기에 추가하면 됩니다)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# 4. 의존성 파일 복사 및 설치
# (캐시 활용을 위해 소스 코드보다 먼저 복사합니다)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 5. 소스 코드 전체 복사
COPY . .

# 6. 포트 8000번 노출 (컨테이너 내부 포트)
EXPOSE 8000

# 7. 서버 실행 (Uvicorn)
# 호스트는 0.0.0.0으로 열어줘야 외부(도커 네트워크 등)에서 접속 가능
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
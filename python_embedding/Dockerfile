# 가볍고 호환성 좋은 Python 3.9 Slim 버전 사용
FROM python:3.9-slim

# 작업 디렉토리 설정
WORKDIR /app

# 1. 의존성 파일 복사 및 설치
COPY requirements.txt .

# --no-cache-dir: 캐시를 남기지 않아 이미지 크기 줄임
RUN pip install --no-cache-dir -r requirements.txt

# 2. 소스 코드 복사
COPY . .

# (선택) 모델을 빌드 시점에 미리 다운로드하려면 아래 주석 해제
# 이렇게 하면 컨테이너 실행 때마다 다운로드받지 않아서 시작이 빠름
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('jhgan/ko-sroberta-multitask')"

# Flask/Gunicorn 포트 노출
EXPOSE 5000

# Gunicorn으로 서버 실행 (워커 1개, 타임아웃 120초)
# app:app -> app.py 파일의 app 객체 실행
CMD ["gunicorn", "-w", "1", "-b", "0.0.0.0:5000", "--timeout", "120", "app:app"]
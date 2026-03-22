# Rerank API (Cross-Encoder)

기본 모델: `cross-encoder/ms-marco-MiniLM-L-6-v2`. 환경 변수로 변경 가능.

### 사용 방법

1. **환경 변수 (선택)**
   - **파인튜닝 모델** (레시피용 학습된 모델):
     ```bash
     export RERANK_MODEL="/Users/idonghyeon/Desktop/캡스톤/capstone_backend/experiments/output/rerank-recipe"
     ```
     또는 `fastapi` 폴더에서 실행할 때:
     ```bash
     export RERANK_MODEL="../experiments/output/rerank-recipe"
     ```
   - 기본 MiniLM 쓰려면 설정 안 하면 됨 (`cross-encoder/ms-marco-MiniLM-L-6-v2`).

2. **서버 실행**
   ```bash
   uvicorn main:app --host 0.0.0.0 --port 8001
   ```
   첫 `/rerank` 호출 시 모델을 로드(또는 HuggingFace에서 다운로드)합니다.

3. **실험**
   Backend·FastAPI 재기동 후 `run_experiment1_ablation.py` 등으로 HYBRID_RERANK·AGENTIC 지표를 측정할 수 있습니다.

### 모델 선택

| RERANK_MODEL | 설명 |
|--------------|------|
| (비설정) 또는 `cross-encoder/ms-marco-MiniLM-L-6-v2` | MiniLM (영문 위주) |
| 로컬 디렉터리 경로 | 파인튜닝한 CrossEncoder (config.json에 model_type 보강됨) |

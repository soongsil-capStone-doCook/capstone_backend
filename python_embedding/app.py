import os
import re
import logging
from collections import Counter
from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer
import torch

# Sparse 벡터 차원 (해시 공간). Qdrant 문서 인덱싱 시 동일 값 사용 필요
SPARSE_VOCAB_SIZE = 100_000

def build_sparse_vector(text: str):
    """
    키워드 신호용 sparse 벡터 (dense와 다른 신호).
    공백/구두점 기준 토큰화 후 해시로 인덱스, 값은 sqrt(tf)로 BM25 스타일.
    """
    if not text or not text.strip():
        return [], []
    text = re.sub(r"\s+", " ", text.strip().lower())
    tokens = [t for t in re.split(r"[\s.,!?]+", text) if len(t) >= 1]
    if not tokens:
        return [], []
    tf = Counter(tokens)
    indices = []
    values = []
    for term, count in tf.items():
        idx = hash(term) % SPARSE_VOCAB_SIZE
        if idx < 0:
            idx += SPARSE_VOCAB_SIZE
        indices.append(idx)
        values.append(float((count ** 0.5)))
    return indices, values

# 1. 로깅 설정 (배포 시 필수)
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

def create_app():
    app = Flask(__name__)

    # 2. 모델 로드 (전역 변수로 한 번만 로드하여 메모리 절약)
    # GPU 사용 가능 시 GPU 할당, 아니면 CPU
    device = "cuda" if torch.cuda.is_available() else "cpu"
    model_name = os.getenv('MODEL_NAME', 'jhgan/ko-sroberta-multitask')
    
    logger.info(f"Loading model: {model_name} on {device}...")
    try:
        model = SentenceTransformer(model_name, device=device)
        logger.info("Model loaded successfully.")
    except Exception as e:
        logger.error(f"Failed to load model: {e}")
        raise e

    @app.route('/health', methods=['GET'])
    def health_check():
        """로드 밸런서 등을 위한 헬스 체크용 엔드포인트"""
        return jsonify({'status': 'healthy'}), 200

    @app.route('/embed', methods=['POST'])
    def embed():
        try:
            # 3. 입력 데이터 검증
            if not request.is_json:
                return jsonify({'error': 'Request must be JSON'}), 400
            
            data = request.json
            text = data.get('text')

            if not text or not isinstance(text, str):
                logger.warning("Invalid input received: text is empty or not a string")
                return jsonify({'error': 'Invalid input: "text" field is required and must be a string'}), 400

            # 4. 벡터 변환 및 예외 처리
            vector = model.encode(text).tolist()
            
            # 로그에는 텍스트 길이 정도만 남김 (개인정보 보호 및 로그 용량 관리)
            logger.info(f"Encoded text of length {len(text)}")
            
            return jsonify({'vector': vector})

        except Exception as e:
            logger.error(f"Error during embedding: {e}")
            return jsonify({'error': 'Internal Server Error'}), 500

    @app.route('/hybrid', methods=['POST'])
    def hybrid():
        """
        Hybrid 검색용: dense(기존 모델) + sparse(키워드 가중치).
        Backend getHybridEmbedding()에서 이 URL 사용 시 DENSE와 다른 순위 가능.
        Qdrant 레시피 인덱싱 시에도 동일 엔드포인트로 문서 sparse 벡터 생성 필요.
        """
        try:
            if not request.is_json:
                return jsonify({'error': 'Request must be JSON'}), 400
            data = request.json
            text = data.get('text')
            if not text or not isinstance(text, str):
                return jsonify({'error': 'Invalid input: "text" is required'}), 400

            dense = model.encode(text).tolist()
            indices, values = build_sparse_vector(text)
            sparse = {"indices": indices, "values": values}

            logger.info(f"Hybrid encoded len(text)=%d dense_dim=%d sparse_nnz=%d", len(text), len(dense), len(indices))
            return jsonify({"dense": dense, "sparse": sparse})
        except Exception as e:
            logger.error(f"Error during hybrid embedding: {e}")
            return jsonify({'error': 'Internal Server Error'}), 500

    return app

# Gunicorn이 이 객체를 찾아서 실행합니다.
app = create_app()

if __name__ == '__main__':
    # 로컬 테스트용 (배포 시에는 실행되지 않음)
    # 맥에서 5000은 AirPlay(ControlCenter)가 사용 → 기본값 5001
    app.run(host='0.0.0.0', port=int(os.getenv('PORT', 5001)), debug=False)
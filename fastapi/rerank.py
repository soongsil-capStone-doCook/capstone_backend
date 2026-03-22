"""
Cross-Encoder Reranker: query와 document 쌍에 대해 관련도 점수를 매기고 순서 반환.
sentence-transformers CrossEncoder (MiniLM 등, RERANK_MODEL로 지정).
"""
import json
import os
from pathlib import Path
from typing import List, Tuple

# Lazy load
_rerank_model = None


def _resolve_local_model_path(model_name: str) -> str:
    """
    로컬 디렉터리인 경우:
    - sentence_transformers가 하위 폴더(0_Transformer 등)에 저장했으면 그 경로 반환
    - config.json에 model_type이 없으면 'bert' 추가 후 해당 경로 반환 (MiniLM 계열)
    """
    path = Path(model_name).resolve()
    if not path.is_dir():
        return model_name

    config_path = path / "config.json"
    # 1) 하위 폴더에 config.json + model_type 있는지 확인 (fit() 저장 구조)
    for sub in sorted(path.iterdir()):
        if sub.is_dir():
            sub_config = sub / "config.json"
            if sub_config.is_file():
                try:
                    with open(sub_config, "r", encoding="utf-8") as f:
                        cfg = json.load(f)
                    if cfg.get("model_type"):
                        print(f"[Rerank] 하위 모델 경로 사용: {sub}")
                        return str(sub)
                except Exception:
                    pass

    # 2) 현재 경로의 config.json에 model_type 보강
    if config_path.is_file():
        try:
            with open(config_path, "r", encoding="utf-8") as f:
                cfg = json.load(f)
            if not cfg.get("model_type"):
                cfg["model_type"] = "bert"
                with open(config_path, "w", encoding="utf-8") as f:
                    json.dump(cfg, f, indent=2, ensure_ascii=False)
                print(f"[Rerank] config.json에 model_type=bert 추가함: {config_path}")
        except Exception as e:
            print(f"[Rerank] config 보강 실패: {e}")
    return str(path)


def _get_model():
    global _rerank_model
    if _rerank_model is not None:
        return _rerank_model

    from sentence_transformers import CrossEncoder

    model_name = (os.getenv("RERANK_MODEL") or "cross-encoder/ms-marco-MiniLM-L-6-v2").strip()
    if model_name and os.path.isdir(model_name):
        model_name = _resolve_local_model_path(model_name)
        model_name = os.path.abspath(model_name)
        print(f"[Rerank] 로컬 모델 로드: {model_name}")

    try:
        _rerank_model = CrossEncoder(model_name)
    except Exception as e:
        print(f"[Rerank] 모델 로드 실패: {model_name} | {e}")
        raise
    return _rerank_model


def rerank(query: str, documents: List[Tuple[int, str]], top_k: int = 10) -> List[int]:
    """
    query와 각 document 텍스트의 유사도 점수로 정렬 후 상위 top_k개의 id 반환.

    :param query: 사용자 질의 (또는 rewritten query)
    :param documents: [(id, text), ...]  (recipe_id, recipe title+description 등)
    :param top_k: 반환할 상위 개수
    :return: ranked recipe ids (관련도 높은 순)
    """
    if not query or not documents:
        return [d[0] for d in documents[:top_k]]

    model = _get_model()
    pairs = [(query, text) for _, text in documents]
    scores = model.predict(pairs)
    # numpy/tensor → float 리스트
    if hasattr(scores, "tolist"):
        scores = scores.tolist()
    scores = [float(s) for s in scores]

    # (id, score) 리스트를 점수 내림차순 정렬
    id_score = [(documents[i][0], scores[i]) for i in range(len(documents))]
    id_score.sort(key=lambda x: x[1], reverse=True)

    return [id for id, _ in id_score[:top_k]]

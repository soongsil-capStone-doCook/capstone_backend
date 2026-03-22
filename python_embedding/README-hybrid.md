# Hybrid 임베딩 (Dense + Sparse)

DENSE와 HYBRID가 다른 결과를 내려면 **sparse를 dense와 다른 신호**로 써야 합니다.

## 1. 엔드포인트

| 경로 | 응답 | 용도 |
|------|------|------|
| `POST /embed` | `{ "vector": [...] }` | Dense만 (기존) |
| `POST /hybrid` | `{ "dense": [...], "sparse": { "indices": [...], "values": [...] } }` | Hybrid 검색용 |

- **sparse**: 쿼리/문서 텍스트를 토큰화한 뒤 해시 인덱스 + `sqrt(tf)` 값. (dense와 별도 신호)

## 2. Backend 설정

Hybrid 검색을 쓰려면 `app.embedding.url`을 **hybrid 전용 URL**로 두면 됩니다.

- 기존: `http://서버:5001/embed` (dense만 받는 서버면 DENSE=HYBRID 현상 발생)
- 변경: `http://서버:5001/hybrid`  
  (이 서버에 `python_embedding`의 `/hybrid` 코드가 배포된 경우)

예: `application.yml` 또는 환경 변수

```yaml
app:
  embedding:
    url: http://localhost:5001/hybrid   # 또는 실제 임베딩 서버 주소/hybrid
```

## 3. Qdrant 레시피 인덱싱

**중요:** Hybrid 검색이 제대로 동작하려면 Qdrant `recipes` 컬렉션에 **벡터가 두 종류** 있어야 합니다.

- `dense`: 기존처럼 SentenceTransformer 등으로 생성
- `sparse`: **이 서버의 `/hybrid`와 동일한 방식**으로 생성해 저장  
  (같은 `build_sparse_vector` 로직 또는 같은 `/hybrid` 호출로 문서 텍스트 → `indices`/`values`)

지금 컬렉션에 `sparse` 벡터가 없거나, dense를 그대로 sparse로 넣었다면:

1. 컬렉션에 `sparse` 네임드 벡터 추가 후
2. 레시피 문서를 다시 인덱싱할 때 **제목/설명 등 텍스트로 `/hybrid`(또는 동일 sparse 로직) 호출**해서 받은 `sparse`를 함께 저장해야 합니다.

인덱싱 스크립트/서비스가 따로 있다면, 그쪽에서도 **문서용 sparse**를 위와 같은 방식으로 생성해 넣어야 DENSE vs HYBRID 순위가 달라집니다.

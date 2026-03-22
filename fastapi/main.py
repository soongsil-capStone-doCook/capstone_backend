from fastapi import FastAPI, UploadFile, File, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from schemas import OcrRes

from ocr_vision import vision_ocr_text
from receipt_parser import parse_receipt_items
from normalize import normalize_items_with_rules_and_llm
from llm_place import InputItem, place_items
from rerank import rerank as cross_encoder_rerank

app = FastAPI()


class PlaceReq(BaseModel):
    items: List[InputItem]


class RerankDoc(BaseModel):
    id: int
    text: str


class RerankReq(BaseModel):
    query: str
    documents: List[RerankDoc]


class RerankRes(BaseModel):
    ranked_ids: List[int]

@app.get("/health")
def health():
    return {"ok": True}

@app.post("/ocr", response_model=OcrRes)
async def ocr(image: UploadFile = File(...)):
    try:
        img_bytes = await image.read()
        raw_text = vision_ocr_text(img_bytes)

        # 1) 영수증 raw_text -> 후보 라인(품목 후보) 추출
        candidates = parse_receipt_items(raw_text)

        # 2) (룰 -> 안되면 LLM) 정규화
        norm_items = normalize_items_with_rules_and_llm(candidates)

        return OcrRes(rawText=raw_text, items=norm_items)

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/place")
def place(req: PlaceReq):
    try:
        return place_items(req.items).model_dump()
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/rerank", response_model=RerankRes)
def rerank_endpoint(req: RerankReq):
    """Cross-Encoder Re-ranking: query와 documents 유사도로 정렬된 recipe id 목록 반환."""
    try:
        documents = [(d.id, d.text) for d in req.documents]
        ranked_ids = cross_encoder_rerank(req.query, documents, top_k=min(50, len(documents)))
        return RerankRes(ranked_ids=ranked_ids)
    except Exception as e:
        import traceback
        print("[Rerank 500]", traceback.format_exc())
        raise HTTPException(status_code=500, detail=str(e))

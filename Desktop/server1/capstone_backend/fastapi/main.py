from fastapi import FastAPI, UploadFile, File, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from schemas import OcrRes

from ocr_vision import vision_ocr_text
from receipt_parser import parse_receipt_items
from normalize import normalize_items_with_rules_and_llm
from llm_place import InputItem, place_items

app = FastAPI()


class PlaceReq(BaseModel):
    items: List[InputItem]

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

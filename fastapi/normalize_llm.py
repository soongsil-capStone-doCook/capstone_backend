# normalize_llm.py
import os
from typing import List, Dict, Tuple
from pydantic import BaseModel, Field, ValidationError
from openai import OpenAI


MODEL = os.getenv("OPENAI_MODEL", "gpt-4o-mini")

ALLOWED_CATS = ["채소","과일","육류","해산물","유제품","소스","양념","음료","냉동식품","기타"]


class LlmNormItem(BaseModel):
    rawName: str
    name: str
    storageCategory: str


class LlmNormRes(BaseModel):
    items: List[LlmNormItem] = Field(default_factory=list)


def llm_normalize_batch(raw_names: List[str]) -> Dict[str, Tuple[str, str]]:
    """
    raw -> (name, category). OPENAI_API_KEY 없으면 LLM 스킵하고 빈 dict 반환.
    """
    if not raw_names:
        return {}

    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key or not api_key.strip():
        return {}

    client = OpenAI(api_key=api_key)

    # 입력 크기 과하면 쪼개도 됨(데모면 보통 필요없음)
    prompt = f"""
너는 영수증 품목명을 '표준 재료명'으로 정규화하는 도우미야.
아래 rawName들을 보고:
- name: 표준 재료명 (예: 한우불고기용 -> 소고기, 서울우유 -> 우유, 닭가슴살 -> 닭고기)
- storageCategory: 다음 중 하나만 선택 {ALLOWED_CATS}
- name에는 절대 '과일/채소/유제품' 같은 카테고리 단어만 쓰지 말고, 실제 품목명(딸기/사과/양파/우유 등)으로 써.

반드시 JSON만 출력해. 설명 금지.
형식:
{{"items":[{{"rawName":"...","name":"...","storageCategory":"..."}}]}}

rawName 목록:
{raw_names}
""".strip()

    resp = client.chat.completions.create(
        model=MODEL,
        temperature=0,
        messages=[
            {"role": "system", "content": "You output ONLY valid JSON."},
            {"role": "user", "content": prompt},
        ],
        response_format={"type": "json_object"},
    )

    text = resp.choices[0].message.content
    try:
        parsed = LlmNormRes.model_validate_json(text)
    except ValidationError:
        # JSON이 좀 깨졌거나 형식 안 맞으면 그냥 전부 실패 처리
        return {}

    out: Dict[str, Tuple[str, str]] = {}
    for it in parsed.items:
        cat = it.storageCategory.strip()
        if cat not in ALLOWED_CATS:
            continue
        out[it.rawName] = (it.name.strip(), cat)
    return out

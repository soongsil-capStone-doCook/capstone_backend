# llm_place.py
import os
import time
import random
import json
from typing import List, Literal, Optional, Any, Dict, Tuple

from pydantic import BaseModel, Field
from openai import OpenAI

# ---------- Config ----------
MODEL = os.getenv("OPENAI_MODEL", "gpt-4.1-nano")
MAX_RETRIES = int(os.getenv("OPENAI_MAX_RETRIES", "5"))

client = OpenAI()  # reads OPENAI_API_KEY automatically


# ---------- Enums (must match your Spring enum names) ----------
FridgeSlot = Literal[
    "FREEZER_TOP",
    "MAIN_SHELF_1", "MAIN_SHELF_2", "MAIN_SHELF_3",
    "CRISPER_DRAWER",
    "DOOR_SHELF_1", "DOOR_SHELF_2", "DOOR_SHELF_3", "DOOR_SHELF_4",
]

StorageCategory = Literal[
    "채소", "과일", "육류", "해산물", "유제품",
    "소스", "양념", "음료", "냉동식품", "기타"
]


# ---------- Schemas ----------
class InputItem(BaseModel):
    # Spring에서 보내는 ingredient id (필수)
    id: int
    name: str
    quantity: Optional[str] = None
    storageCategory: Optional[str] = None  # optional hint

class Placement(BaseModel):
    id: int
    name: str
    storageCategory: StorageCategory
    fridgeSlot: FridgeSlot

class Unplaced(BaseModel):
    id: int
    name: str
    reason: str

class PlaceResult(BaseModel):
    placements: List[Placement] = Field(default_factory=list)
    unplaced: List[Unplaced] = Field(default_factory=list)


# ---------- Rules prompt ----------
RULES = """
You are a fridge organizer.
Return JSON ONLY that matches the given schema.

Rules:
- FREEZER_TOP: frozen foods (e.g., frozen dumplings, ice cream, frozen meat, frozen vegetables).
- CRISPER_DRAWER: fruits and vegetables.
- MAIN_SHELF_3 (bottom shelf): raw meat/poultry/seafood (prevent drips/cross-contamination).
- MAIN_SHELF_2 (middle shelf): dairy and eggs (stable temperature; avoid door).
- MAIN_SHELF_1 (top shelf): ready-to-eat foods / leftovers / prepared foods.
- DOOR_SHELF_*: sauces/condiments and drinks (door temperature fluctuates).

You MUST fill storageCategory and fridgeSlot for every placement. Never output null for them.
If an item is ambiguous, put it in "unplaced" with a short reason.
"""


# ---------- JSON schema strict helpers ----------
def _enforce_additional_properties_false(schema: Any) -> Any:
    """Recursively set additionalProperties=False for all objects."""
    if isinstance(schema, dict):
        if schema.get("type") == "object":
            schema["additionalProperties"] = False

        if "properties" in schema and isinstance(schema["properties"], dict):
            for v in schema["properties"].values():
                _enforce_additional_properties_false(v)

        if "items" in schema:
            _enforce_additional_properties_false(schema["items"])

        for key in ["allOf", "anyOf", "oneOf"]:
            if key in schema and isinstance(schema[key], list):
                for v in schema[key]:
                    _enforce_additional_properties_false(v)

        if "$defs" in schema and isinstance(schema["$defs"], dict):
            for v in schema["$defs"].values():
                _enforce_additional_properties_false(v)

        for v in schema.values():
            _enforce_additional_properties_false(v)

    elif isinstance(schema, list):
        for v in schema:
            _enforce_additional_properties_false(v)

    return schema


def _make_nullable(prop_schema: Any) -> Any:
    """Wrap schema to allow null without breaking existing constraints."""
    if not isinstance(prop_schema, dict):
        return prop_schema

    if "anyOf" in prop_schema and isinstance(prop_schema["anyOf"], list):
        if any(isinstance(x, dict) and x.get("type") == "null" for x in prop_schema["anyOf"]):
            return prop_schema
        return {"anyOf": prop_schema["anyOf"] + [{"type": "null"}]}

    t = prop_schema.get("type")
    if t == "null":
        return prop_schema

    if isinstance(t, list):
        if "null" in t:
            return prop_schema
        new = dict(prop_schema)
        new["type"] = t + ["null"]
        return new

    return {"anyOf": [prop_schema, {"type": "null"}]}


def _ensure_required_all_properties_and_nullable(schema: Any) -> Any:
    """
    OpenAI strict json_schema requires:
    - required must exist for every object
    - required must include every key in properties
    To keep "optional-like" behavior, we allow null for each property.
    """
    if isinstance(schema, dict):
        if schema.get("type") == "object" and isinstance(schema.get("properties"), dict):
            props: Dict[str, Any] = schema["properties"]
            all_keys = list(props.keys())
            schema["required"] = all_keys

            for k, v in props.items():
                props[k] = _make_nullable(v)

        if "properties" in schema and isinstance(schema["properties"], dict):
            for v in schema["properties"].values():
                _ensure_required_all_properties_and_nullable(v)

        if "items" in schema:
            _ensure_required_all_properties_and_nullable(schema["items"])

        for key in ["allOf", "anyOf", "oneOf"]:
            if key in schema and isinstance(schema[key], list):
                for v in schema[key]:
                    _ensure_required_all_properties_and_nullable(v)

        if "$defs" in schema and isinstance(schema["$defs"], dict):
            for v in schema["$defs"].values():
                _ensure_required_all_properties_and_nullable(v)

    elif isinstance(schema, list):
        for v in schema:
            _ensure_required_all_properties_and_nullable(v)

    return schema


# ---------- Fallback rules ----------
def _fallback_rule(name: str) -> Optional[Tuple[str, str]]:
    n = (name or "").strip().lower()

    freezer_keys = ["냉동", "만두", "아이스", "ice", "frozen"]
    if any(k in n for k in freezer_keys):
        return ("냉동식품", "FREEZER_TOP")

    veg_keys = ["상추", "깻잎", "양파", "파", "오이", "당근", "감자", "배추", "버섯", "시금치", "브로콜리", "토마토"]
    fruit_keys = ["사과", "배", "바나나", "딸기", "포도", "귤", "오렌지", "키위", "복숭아", "망고"]
    if any(k in n for k in veg_keys):
        return ("채소", "CRISPER_DRAWER")
    if any(k in n for k in fruit_keys):
        return ("과일", "CRISPER_DRAWER")

    dairy_keys = ["우유", "치즈", "요거트", "요구르트", "버터", "크림", "계란", "달걀", "milk", "cheese", "yogurt", "egg"]
    if any(k in n for k in dairy_keys):
        return ("유제품", "MAIN_SHELF_2")

    meat_keys = ["돼지", "소고기", "닭", "삼겹", "목살", "스테이크", "beef", "pork", "chicken"]
    seafood_keys = ["새우", "오징어", "문어", "조개", "굴", "연어", "참치", "fish", "shrimp", "salmon", "tuna"]
    if any(k in n for k in meat_keys):
        return ("육류", "MAIN_SHELF_3")
    if any(k in n for k in seafood_keys):
        return ("해산물", "MAIN_SHELF_3")

    sauce_keys = ["간장", "고추장", "된장", "케첩", "마요", "소스", "드레싱", "잼", "머스타드", "ketchup", "mayo", "sauce"]
    drink_keys = ["음료", "콜라", "사이다", "주스", "물", "coffee", "tea", "juice", "coke", "water"]
    if any(k in n for k in sauce_keys):
        return ("소스", "DOOR_SHELF_2")
    if any(k in n for k in drink_keys):
        return ("음료", "DOOR_SHELF_2")

    return None


def _infer_category_and_slot(name: str) -> Tuple[str, str]:
    r = _fallback_rule(name)
    if r is None:
        return ("기타", "MAIN_SHELF_1")
    return r


def _normalize_llm_output(obj: dict) -> dict:
    # placements 보정: storageCategory/fridgeSlot이 null/""이면 fallback으로 채움
    placements = obj.get("placements") or []
    for p in placements:
        nm = p.get("name") or ""
        if p.get("storageCategory") in (None, ""):
            cat, _ = _infer_category_and_slot(nm)
            p["storageCategory"] = cat
        if p.get("fridgeSlot") in (None, ""):
            _, slot = _infer_category_and_slot(nm)
            p["fridgeSlot"] = slot
        # id가 null이면 unplaced로 보내는 게 맞지만, 여기선 일단 -1로 채움(스키마 만족)
        if p.get("id") is None:
            p["id"] = -1

    unplaced = obj.get("unplaced") or []
    for u in unplaced:
        if u.get("reason") in (None, ""):
            u["reason"] = "missing_reason"
        if u.get("id") is None:
            u["id"] = -1

    obj["placements"] = placements
    obj["unplaced"] = unplaced
    return obj


def _is_retryable_error(e: Exception) -> bool:
    msg = str(e).lower()
    return ("429" in msg) or ("rate" in msg) or ("503" in msg) or ("timeout" in msg) or ("temporarily" in msg)


# ---------- Main function ----------
def place_items(items: List[InputItem]) -> PlaceResult:
    payload = {"items": [i.model_dump() for i in items]}

    # Build strict JSON schema for OpenAI structured outputs
    base_schema = PlaceResult.model_json_schema()
    strict_schema = _enforce_additional_properties_false(base_schema)
    strict_schema = _ensure_required_all_properties_and_nullable(strict_schema)

    schema = {
        "name": "place_result",
        "strict": True,
        "schema": strict_schema,
    }

    last_err: Optional[Exception] = None

    for attempt in range(MAX_RETRIES):
        try:
            resp = client.chat.completions.create(
                model=MODEL,
                messages=[
                    {"role": "system", "content": RULES},
                    {"role": "user", "content": f"Input:\n{payload}"},
                ],
                response_format={"type": "json_schema", "json_schema": schema},
                temperature=0,
            )

            text = resp.choices[0].message.content  # JSON string
            raw = json.loads(text)
            raw = _normalize_llm_output(raw)

            return PlaceResult.model_validate(raw)

        except Exception as e:
            last_err = e
            if _is_retryable_error(e):
                time.sleep((0.7 * (2 ** attempt)) + random.uniform(0, 0.3))
                continue
            # 실패 시에도 서비스 계속 돌리려면 fallback으로 내려가게 함
            break

    # fallback (LLM 실패/쿼터/기타 에러 시)
    placements: List[Placement] = []
    unplaced: List[Unplaced] = []

    for it in items:
        r = _fallback_rule(it.name)
        if r is None:
            unplaced.append(Unplaced(
                id=it.id,
                name=it.name,
                reason=f"fallback_no_rule ({type(last_err).__name__})"
            ))
        else:
            cat, slot = r
            placements.append(Placement(
                id=it.id,
                name=it.name,
                storageCategory=cat,   # type: ignore
                fridgeSlot=slot        # type: ignore
            ))

    return PlaceResult(placements=placements, unplaced=unplaced)

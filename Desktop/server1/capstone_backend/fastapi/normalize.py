# normalize.py
import os
import re
from typing import List, Tuple

from receipt_parser import Candidate
from normalize_rules import rule_normalize_one
from normalize_llm import llm_normalize_batch
from schemas import OcrItem


# -----------------------------
# Non-food / noise filtering
# -----------------------------
NON_FOOD_KEYS = [
    "휴지","키친타월","물티슈","세제","섬유유연제","샴푸","린스","바디워시","비누","치약","칫솔",
    "생리대","기저귀","면도기","마스크","장갑","수건","행주","랩","호일","지퍼백","빨대","컵",
    "건전지","배터리","충전","전구","세정","살균","소독","방향제","탈취","스프레이",
    "세탁","청소","주방세제","락스"
]

HEADER_NOISE_PATTERNS = [
    r"^\[",          # [구매] 같은 헤더
    r"^※",           # ※ 안내문
    r"^pos",         # POS:
    r"승인", r"카드", r"현금", r"합계", r"총", r"부가세", r"과세", r"면세", r"거스름",
    r"사업자", r"매장", r"주소", r"전화", r"대표", r"가맹점", r"승인번호", r"거래", r"포인트"
]

def is_non_food_line(raw: str) -> bool:
    s = (raw or "").strip()
    if not s:
        return True

    low = s.lower()

    # 헤더/안내문 패턴
    for p in HEADER_NOISE_PATTERNS:
        if re.search(p, low):
            return True

    # 생활용품 키워드
    if any(k in low for k in NON_FOOD_KEYS):
        return True

    return False


# -----------------------------
# Main normalization pipeline
# -----------------------------
def normalize_items_with_rules_and_llm(candidates: List[Candidate]) -> List[OcrItem]:
    """
    1) rule_normalize_one()으로 빠르게 정규화
    2) category가 '기타'인 것만 LLM으로 정규화(옵션)
    3) 식품 아닌 라인 제거 + 기타 제거(기본)
    """
    items: List[OcrItem] = []
    unknown: List[Tuple[int, Candidate]] = []

    # 1) 룰 기반 정규화
    for idx, c in enumerate(candidates):
        canonical_name, cat = rule_normalize_one(c.raw)

        if cat == "기타":
            unknown.append((idx, c))

        items.append(
            OcrItem(
                rawName=c.raw,
                name=canonical_name,
                quantity=c.qty_hint,
                expiryDate=None,
                storageCategory=cat,
            )
        )

    # 2) 룰로 못 잡은 것만 LLM 정규화(옵션)
    use_llm = os.getenv("USE_LLM_NORMALIZE", "1") == "1"
    if use_llm and unknown:
        raw_list = [c.raw for _, c in unknown]
        llm_map = llm_normalize_batch(raw_list)  # raw -> (name, category)

        for idx, c in unknown:
            if c.raw not in llm_map:
                continue
            name, cat = llm_map[c.raw]
            if name and cat:
                items[idx].name = name
                items[idx].storageCategory = cat

    # 3) 필터링: 비식품 제거 + 기타 제거
    filtered: List[OcrItem] = []
    for it in items:
        if is_non_food_line(it.rawName):
            continue
        # storageCategory가 기타인데 식품 확신이 없으면 제거(기본 정책)
        if it.storageCategory == "기타":
            continue
        filtered.append(it)

    return filtered

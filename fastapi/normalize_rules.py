# normalize_rules.py
import re
from typing import Tuple

ALLOWED_CATS = ["채소","과일","육류","해산물","유제품","소스","양념","음료","냉동식품","기타"]

def _clean(s: str) -> str:
    s = re.sub(r"\(.*?\)", " ", s)  # 괄호 제거
    s = re.sub(r"[^가-힣A-Za-z0-9\s]", " ", s)
    s = re.sub(r"\s{2,}", " ", s).strip()
    return s

# 구체 품목 키워드 매핑 (여기 계속 늘리면 정확도 오름)
FRUIT_MAP = {
    "산딸기": "딸기",
    "딸기": "딸기",
    "블루베리": "블루베리",
    "포도": "포도",
    "사과": "사과",
    "바나나": "바나나",
    "키위": "키위",
    "귤": "귤",
    "오렌지": "오렌지",
    "망고": "망고",
    "복숭아": "복숭아",
    "배": "배",
}

VEG_MAP = {
    "상추": "상추",
    "깻잎": "깻잎",
    "양파": "양파",
    "대파": "대파",
    "쪽파": "쪽파",
    "오이": "오이",
    "당근": "당근",
    "감자": "감자",
    "배추": "배추",
    "버섯": "버섯",
    "시금치": "시금치",
    "브로콜리": "브로콜리",
    "토마토": "토마토",
}

def rule_normalize_one(raw: str) -> Tuple[str, str]:
    """
    return: (canonical_name, storageCategory)
    """
    cleaned = _clean(raw)
    low = cleaned.lower()

    # ---- 육류 ----
    if any(k in low for k in ["한우","소고기","등심","채끝","안심","양지","국거리","불고기"]):
        return ("소고기", "육류")
    if any(k in low for k in ["돼지","삼겹","목살","앞다리","뒷다리","항정","가브리"]):
        return ("돼지고기", "육류")
    if any(k in low for k in ["닭","닭가슴살","닭다리","닭봉","닭볶음탕"]):
        return ("닭고기", "육류")

    # ---- 해산물 ----
    if any(k in low for k in ["새우","오징어","문어","조개","굴","연어","참치","생선"]):
        return ("해산물", "해산물")

    # ---- 유제품/계란 ----
    if any(k in low for k in ["우유","치즈","요거트","요구르트","버터","크림"]):
        return ("유제품", "유제품")
    if any(k in low for k in ["계란","달걀"]):
        return ("계란", "유제품")

    # ---- 과일: "과일"이 아니라 "딸기/사과..." 반환 ----
    for k, v in FRUIT_MAP.items():
        if k in low:
            return (v, "과일")

    # ---- 채소: "채소"가 아니라 "양파/오이..." 반환 ----
    for k, v in VEG_MAP.items():
        if k in low:
            return (v, "채소")

    # ---- 소스/양념/음료/냉동 ----
    if any(k in low for k in ["간장","고추장","된장","케첩","마요","소스","드레싱","잼","머스타드"]):
        return ("소스", "소스")
    if any(k in low for k in ["소금","설탕","후추","참기름","들기름","식초"]):
        return ("양념", "양념")
    if any(k in low for k in ["콜라","사이다","주스","물","커피","tea","음료"]):
        return ("음료", "음료")
    if any(k in low for k in ["냉동","frozen","ice","아이스","만두"]):
        return ("냉동식품", "냉동식품")

    return (cleaned if cleaned else raw, "기타")

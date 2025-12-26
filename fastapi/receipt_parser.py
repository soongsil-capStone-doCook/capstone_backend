# receipt_parser.py
import re
from typing import List, Optional
from pydantic import BaseModel


class Candidate(BaseModel):
    raw: str
    qty_hint: Optional[str] = None


BLOCK_KEYS = [
    "합계", "총", "과세", "면세", "부가세", "카드", "현금", "승인", "거스름",
    "매장", "사업자", "전화", "주소", "대표", "포인트", "주문", "단가", "금액",
    "영수증", "환불", "취소", "부가", "거래", "승인번호", "가맹점"
]

QTY_RE = re.compile(r"(\d+(?:\.\d+)?\s*(?:kg|g|ml|l|개|봉|팩|통|캔|병))", re.IGNORECASE)
PRICE_RE = re.compile(r"(\d{1,3}(?:,\d{3})+|\d{3,})")  # 1,000 or 3000 같은 숫자 덩어리


def parse_receipt_items(raw_text: str) -> List[Candidate]:
    out: List[Candidate] = []
    if not raw_text:
        return out

    for line in raw_text.splitlines():
        s = line.strip()
        if not s:
            continue

        # 차단 키워드
        if any(k in s for k in BLOCK_KEYS):
            continue

        # 한글/영문 거의 없으면 패스
        if not re.search(r"[가-힣A-Za-z]", s):
            continue

        qty = None
        m = QTY_RE.search(s)
        if m:
            qty = re.sub(r"\s+", "", m.group(1))

        # 가격 숫자 제거(품목명만 남기려는 시도)
        name = PRICE_RE.sub(" ", s)
        name = re.sub(r"\s{2,}", " ", name).strip()

        if len(name) < 2:
            continue

        out.append(Candidate(raw=name, qty_hint=qty))

    return out

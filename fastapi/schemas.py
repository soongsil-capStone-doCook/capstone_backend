from pydantic import BaseModel
from typing import List, Optional

class OcrItem(BaseModel):
    rawName: str
    name: str
    quantity: Optional[str] = None
    expiryDate: Optional[str] = None
    storageCategory: str

class OcrRes(BaseModel):
    rawText: str
    items: List[OcrItem]

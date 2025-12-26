# ocr_vision.py
from google.cloud import vision


def vision_ocr_text(image_bytes: bytes) -> str:
    """
    GOOGLE_APPLICATION_CREDENTIALS 환경변수로 서비스계정 키를 잡는다.
    영수증은 DOCUMENT_TEXT_DETECTION이 일반적으로 더 잘 잡힘.
    """
    client = vision.ImageAnnotatorClient()
    image = vision.Image(content=image_bytes)

    resp = client.document_text_detection(image=image)
    if resp.error.message:
        raise RuntimeError(f"Vision OCR error: {resp.error.message}")

    if resp.full_text_annotation and resp.full_text_annotation.text:
        return resp.full_text_annotation.text

    return ""

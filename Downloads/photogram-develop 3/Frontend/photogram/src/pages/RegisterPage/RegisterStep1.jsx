import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom"; // ✅ 추가

const TERMS = [
  { id: 1, label: "서비스 이용약관에 동의합니다. (필수)" },
  { id: 2, label: "개인정보 수집 및 이용에 동의합니다. (필수)" },
];

function RegisterStep1({ formData, onChange, onNext }) {
  const [checkedTerms, setCheckedTerms] = useState([]);
  const [errors, setErrors] = useState([]);
  const navigate = useNavigate(); // ✅ 추가

  useEffect(() => {
    if (formData.agreedTermIds?.length > 0) {
      setCheckedTerms(formData.agreedTermIds);
    }
  }, [formData.agreedTermIds]);

  const toggleTerm = (termId) => {
    if (checkedTerms.includes(termId)) {
      setCheckedTerms((prev) => prev.filter((id) => id !== termId));
      setErrors((prev) => prev.filter((id) => id !== termId));
    } else {
      setCheckedTerms((prev) => [...prev, termId]);
      setErrors((prev) => prev.filter((id) => id !== termId));
    }
  };

  const handleNextClick = () => {
    const missing = TERMS.map((t) => t.id).filter(
      (id) => !checkedTerms.includes(id)
    );
    if (missing.length > 0) {
      setErrors(missing);
      return;
    }
    onChange("agreedTermIds", checkedTerms);
    onNext();
  };

  return (
    <div className="form">
      <h2>약관 동의</h2>

      {TERMS.map((term) => (
        <label
          key={term.id}
          style={{
            border: errors.includes(term.id) ? "2px solid red" : "none",
            padding: "8px",
            display: "block",
            borderRadius: "4px",
            marginBottom: "8px",
            backgroundColor: "#f9f9f9",
          }}
        >
          <input
            type="checkbox"
            checked={checkedTerms.includes(term.id)}
            onChange={() => toggleTerm(term.id)}
          />{" "}
          {term.label}
        </label>
      ))}

      <button type="button" className="submit-button" onClick={handleNextClick}>
        다음 단계로
      </button>

      {/* ✅ 로그인 유도 문구 추가 */}
      <p className="switch-text">
        이미 계정이 있으신가요?{" "}
        <span
          style={{ color: "#007bff", cursor: "pointer" }}
          onClick={() => navigate("/login")}
        >
          로그인
        </span>
      </p>
    </div>
  );
}

export default RegisterStep1;

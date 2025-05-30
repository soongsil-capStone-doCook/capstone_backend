import React from "react";

function RegisterStep2({ formData, onChange, onNext, onBack }) {
  const handleSubmit = (e) => {
    e.preventDefault();
    onNext(); // 유효성 검사는 브라우저가 수행
  };

  return (
    <form className="form" onSubmit={handleSubmit}>
      <h2>회원 정보 입력</h2>

      <input
        type="text"
        placeholder="포토그램 아이디"
        value={formData.photogramId}
        onChange={(e) => onChange("photogramId", e.target.value)}
        required
      />

      <input
        type="email"
        placeholder="이메일"
        value={formData.email}
        onChange={(e) => onChange("email", e.target.value)}
        required
      />

      <input
        type="password"
        placeholder="비밀번호"
        value={formData.password}
        onChange={(e) => onChange("password", e.target.value)}
        required
      />

      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          marginTop: "20px",
        }}
      >
        <button type="button" className="submit-button" onClick={onBack}>
          이전
        </button>
        <button type="submit" className="submit-button">
          다음
        </button>
      </div>
    </form>
  );
}

export default RegisterStep2;

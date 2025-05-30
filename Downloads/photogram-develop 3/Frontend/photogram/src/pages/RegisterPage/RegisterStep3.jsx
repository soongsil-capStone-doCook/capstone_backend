import React from "react";

function RegisterStep3({ formData, onChange, onSubmit, onBack }) {
  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit();
  };

  const handleImageChange = (e) => {
    onChange("profileImage", e.target.files[0]);
  };

  return (
    <form className="form" onSubmit={handleSubmit}>
      <h2>프로필 설정</h2>

      <input
        type="text"
        placeholder="이름"
        value={formData.name}
        onChange={(e) => onChange("name", e.target.value)}
        required
      />

      <input
        type="text"
        placeholder="닉네임"
        value={formData.nickName}
        onChange={(e) => onChange("nickName", e.target.value)}
        required
      />

      <input
        type="file"
        accept="image/*"
        onChange={handleImageChange}
        style={{ marginBottom: "15px" }}
      />

      <input
        type="text"
        placeholder="한 줄 소개 (선택)"
        value={formData.introIndex}
        onChange={(e) => onChange("introIndex", e.target.value)}
      />

      <select
        value={formData.visibility}
        onChange={(e) => onChange("visibility", e.target.value)}
        required
        style={{
          marginTop: "10px",
          marginBottom: "20px",
          padding: "10px",
          borderRadius: "4px",
          border: "1px solid #ccc",
        }}
      >
        <option value="" disabled hidden>
          공개 범위 선택
        </option>
        <option value="PUBLIC">전체 공개</option>
        <option value="FRIEND">친구 공개</option>
        <option value="PRIVATE">비공개</option>
      </select>

      <div style={{ display: "flex", justifyContent: "space-between" }}>
        <button type="button" className="submit-button" onClick={onBack}>
          이전
        </button>
        <button type="submit" className="submit-button">
          가입하기
        </button>
      </div>
    </form>
  );
}

export default RegisterStep3;

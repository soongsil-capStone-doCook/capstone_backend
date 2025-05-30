// pages/SocialLogin/SetProfilePage.jsx
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import "../LoginPage.css"; // 기존 RegisterPage 스타일 사용

function SetProfilePage() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    nickName: "",
    profileImage: null,
    introIndex: "",
    visibility: "PUBLIC",
  });

  const handleChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleImageChange = (e) => {
    handleChange("profileImage", e.target.files[0]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const token = sessionStorage.getItem("accessToken");
      const data = new FormData();

      // ✅ JSON 객체를 Blob으로 만들어 추가
      const initRequest = {
        nickname: formData.nickName,
        message: formData.introIndex,
        visibility: formData.visibility,
      };
      const jsonBlob = new Blob([JSON.stringify(initRequest)], {
        type: "application/json",
      });
      data.append("initRequest", jsonBlob);

      // ✅ 프로필 이미지 추가
      if (formData.profileImage) {
        data.append("profileImage", formData.profileImage);
      } else {
        // 기본 이미지 로드 (주의: public 폴더는 /부터 시작해야 함)
        const response = await fetch("/images/default_profile.png");
        const blob = await response.blob();
        const defaultFile = new File([blob], "default-profile.png", {
          type: blob.type,
        });
        data.append("profileImage", defaultFile);
      }

      await axios.put("/user/social-init", data, {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "multipart/form-data",
        },
      });

      alert("프로필 설정이 완료되었습니다!");
      navigate("/");
    } catch (err) {
      console.error("프로필 설정 실패:", err);
      alert("설정에 실패했습니다.");
    }
  };

  return (
    <div className="login-page-wrapper">
      <div className="login-container">
        <form className="form" onSubmit={handleSubmit}>
          <h2>프로필 설정</h2>

          <input
            type="text"
            placeholder="닉네임"
            value={formData.nickName}
            onChange={(e) => handleChange("nickName", e.target.value)}
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
            onChange={(e) => handleChange("introIndex", e.target.value)}
          />

          <select
            value={formData.visibility}
            onChange={(e) => handleChange("visibility", e.target.value)}
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

          <button type="submit" className="submit-button">
            설정 완료
          </button>
        </form>
      </div>
    </div>
  );
}

export default SetProfilePage;

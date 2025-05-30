import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import "../LoginPage.css";

// 스텝별 컴포넌트 (개별 파일에서 만들어야 함)
import RegisterStep1 from "./RegisterStep1";
import RegisterStep2 from "./RegisterStep2";
import RegisterStep3 from "./RegisterStep3";

function RegisterPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);

  // ✅ 백엔드 스펙에 맞춘 formData 구조
  const [formData, setFormData] = useState({
    nickName: "",
    name: "",
    profileImage: null,
    backgroundImageUrl: "",
    introIndex: "",
    visibility: "PUBLIC",
    email: "",
    password: "",
    photogramId: "",
    agreedTermIds: [], // ✅ 약관 ID 리스트
  });

  const nextStep = () => setStep((prev) => prev + 1);
  const prevStep = () => setStep((prev) => prev - 1);

  const handleChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // ✅ 회원가입 최종 전송
  const handleSubmit = async () => {
    try {
      const formDataToSend = new FormData();

      // ✅ JSON 형태로 보낼 회원가입 정보 객체
      const signUpRequest = {
        nickName: formData.nickName,
        name: formData.name,
        backgroundImageUrl: formData.backgroundImageUrl,
        introIndex: formData.introIndex,
        visibility: formData.visibility,
        email: formData.email,
        password: formData.password,
        photogramId: formData.photogramId,
        agreedTermIds: formData.agreedTermIds,
      };

      // ✅ JSON을 Blob으로 감싸기
      const jsonBlob = new Blob([JSON.stringify(signUpRequest)], {
        type: "application/json",
      });
      formDataToSend.append("signUpRequest", jsonBlob);

      // ✅ 이미지 파일 처리
      if (formData.profileImage) {
        formDataToSend.append("profileImage", formData.profileImage);
      } else {
        // 기본 이미지 로드 (주의: public 폴더는 /부터 시작해야 함)
        const response = await fetch("/images/default_profile.png");
        const blob = await response.blob();
        const defaultFile = new File([blob], "default-profile.png", {
          type: blob.type,
        });
        formDataToSend.append("profileImage", defaultFile);
      }

      // ✅ 전송
      await axios.post("http://localhost:8080/users/signup", formDataToSend);

      alert("회원가입 성공!");
      navigate("/login");
    } catch (err) {
      console.error(err);
      alert("회원가입 실패");
    }
  };

  return (
    <div className="login-page-wrapper">
      <div className="login-container">
        {step === 1 && (
          <RegisterStep1
            formData={formData}
            onChange={handleChange}
            onNext={nextStep}
          />
        )}
        {step === 2 && (
          <RegisterStep2
            formData={formData}
            onChange={handleChange}
            onNext={nextStep}
            onBack={prevStep}
          />
        )}
        {step === 3 && (
          <RegisterStep3
            formData={formData}
            onChange={handleChange}
            onSubmit={handleSubmit}
            onBack={prevStep}
          />
        )}
      </div>
    </div>
  );
}

export default RegisterPage;

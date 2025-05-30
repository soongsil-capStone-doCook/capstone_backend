// pages/OauthSuccess.jsx
import { useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";

const OauthSuccess = (setIsLogined) => {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const token = params.get("token");

    if (token) {
      sessionStorage.setItem("accessToken", token);
      setIsLogined(true);
      navigate("/"); // 기존 유저는 메인으로
    } else {
      alert("로그인 실패");
      navigate("/login");
    }
  }, [location, navigate, setIsLogined]);

  return <div>로그인 처리 중입니다...</div>;
};

export default OauthSuccess;

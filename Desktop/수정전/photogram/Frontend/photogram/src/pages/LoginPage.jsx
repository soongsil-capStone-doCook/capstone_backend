import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import "./LoginPage.css";
import SocialLoginButtons from "../components/Login/SocialLoginButtons";

function LoginPage() {
  const navigate = useNavigate();
  const [photogramId, setPhotogramId] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const handleLogin = async (e) => {
    e.preventDefault();

    try {
      const response = await axios.post("http://localhost:8080/users/login", {
        photogramId,
        password,
      });

      console.log(response.data); // ✅ 성공하면 응답 데이터 출력
      // 예: 토큰 저장
      sessionStorage.setItem("accessToken", response.data.accessToken);

      // 메인 페이지로 이동
      navigate("/");
    } catch (error) {
      console.error(error);
      setErrorMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
  };

  return (
    <div className="login-page-wrapper">
      <div className="login-container">
        <form className="form" onSubmit={handleLogin}>
          <h2>로그인</h2>
          <input
            type="photogramId"
            placeholder="포토그램 아이디"
            value={photogramId}
            onChange={(e) => setPhotogramId(e.target.value)}
            required
          />
          <input
            type="password"
            placeholder="비밀번호"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <button type="submit" className="submit-button">
            로그인
          </button>

          {errorMessage && <p style={{ color: "red" }}>{errorMessage}</p>}

          <SocialLoginButtons />

          <p className="switch-text">
            계정이 없으신가요?{" "}
            <span onClick={() => navigate("/register")}>회원가입</span>
          </p>
        </form>
      </div>
    </div>
  );
}

export default LoginPage;

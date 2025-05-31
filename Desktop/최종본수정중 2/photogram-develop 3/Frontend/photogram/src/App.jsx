import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import "./App.css";
import Header from "./components/Header/Header";
import Footer from "./components/Footer/Footer";
import MainPage from "./pages/MainPage";
import MyPage from "./pages/MyPage";
import { useState, useEffect } from "react";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage/RegisterPage";
import SetProfilePage from "./pages/SocialLogin/SetProfilePage";
import OauthSuccess from "./pages/SocialLogin/OauthSuccess";

function App() {
  const [userData, setUserData] = useState(null);
  const [isLogined, setIsLogined] = useState(
    !!sessionStorage.getItem("accessToken")
  );

  useEffect(() => {
    const token = sessionStorage.getItem("accessToken");

    if (!token) {
      setUserData(null);
      setIsLogined(false);
      return;
    }

    fetch("http://localhost:8080/users/me", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
      .then((res) => {
        if (!res.ok) throw new Error("인증 실패");
        return res.json();
      })
      .then((data) => {
        setUserData(data);
        setIsLogined(true);
      })
      .catch((err) => {
        console.error("유저 정보 로딩 실패:", err);
        sessionStorage.removeItem("accessToken");
        setUserData(null);
        setIsLogined(false);
      });
  }, [isLogined]);

  // 임시 로그인용 함수
  const handleFakeLogin = async () => {
    try {
      const token = "Bearer your_dummy_token";
      localStorage.setItem("token", token);
      const res = await fetch("/users/me", {
        headers: {
          Authorization: token,
        },
      });
      const data = await res.json();
      setUserData(data);
    } catch (err) {
      console.error("로그인 실패:", err);
    }
  };

  return (
    <Router>
      <Header
          isLogined={isLogined}
          setIsLogined={setIsLogined}
          onFakeLogin={handleFakeLogin}
          userData={userData}
      />
      <Routes>
        <Route
          path="/"
          element={
            <MainPage userData={userData} isLogined={isLogined} setIsLogined={setIsLogined} />
          }
        />
        {/* ✅ 닉네임 기반 마이페이지 라우팅 */}
        <Route path="/:nickname" element={<MyPage currentUser={userData} />} />
        {/* ✅ prop으로 전달 */}
        {/* props 추가 */}
        <Route path="/login" element={<LoginPage setIsLogined={setIsLogined}/>} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/oauth-success"
          element={<OauthSuccess setIsLogined={setIsLogined} />}
        />
      </Routes>
      <Footer />
    </Router>
  );
}

export default App;

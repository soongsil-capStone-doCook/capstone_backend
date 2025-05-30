import { Link, useNavigate, useLocation } from "react-router-dom";
import "./Header.css";

const Header = ({
                  isLogined,
                  setIsLogined,
                  onFakeLogin,
                  userData,
                }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const hideLoginButton =
      isLogined ||
      location.pathname === "/login" ||
      location.pathname === "/register";

  const handleLogout = () => {
    sessionStorage.removeItem("accessToken"); // JWT 제거
    setIsLogined(false); // 상태 갱신 (App.jsx에서 올라온 props)
    alert("로그아웃되었습니다.");
    navigate("/"); // 메인페이지로 이동
  };

  const handleMyPageClick = () => {
    if (userData?.nickName) {
      navigate(`/${userData.nickName}`);
    }
  };

  return (
      <header className="header">
        <Link to="/" className="Logo">
          📷 Photogram
        </Link>

        <div className="header-buttons">
          <button className="MyPage" onClick={handleMyPageClick}>
            마이페이지
          </button>

          {!isLogined && !hideLoginButton && (
              <button className="Login" onClick={() => navigate("/login")}>
                로그인
              </button>
          )}

          {isLogined && (
              <button className="Logout" onClick={handleLogout}>
                로그아웃
              </button>
          )}
        </div>
      </header>
  );
};

export default Header;

import React from "react";
import "./SocialLoginButtons.css";
import { FaGoogle, FaApple } from "react-icons/fa";
import { SiNaver, SiKakaotalk } from "react-icons/si";

function SocialLoginButtons() {
  return (
    <div className="social-login-buttons">
      <button
        type="button"
        className="social-button social-google"
        onClick={() => {
          window.location.href =
            "http://localhost:8080/oauth2/authorization/google";
        }}
      >
        <FaGoogle size={20} />
      </button>
    </div>
  );
}

export default SocialLoginButtons;

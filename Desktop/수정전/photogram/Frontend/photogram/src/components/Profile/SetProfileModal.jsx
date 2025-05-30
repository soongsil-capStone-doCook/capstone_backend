import React, { useState, useEffect, useRef } from "react";
import Modal from "react-modal";
import { useNavigate } from "react-router-dom";
import "./SetProfileModal.css";
import { FaCog } from "react-icons/fa";
import axios from "axios";

Modal.setAppElement("#root");

const SetProfileModal = ({
                           isOpen,
                           onRequestClose,
                           onSubmit,
                           currentProfile,
                         }) => {
  const [imagePreview, setImagePreview] = useState(null);
  const [newImage, setNewImage] = useState(null);
  const [nickName, setNickName] = useState("");
  const [introIndex, setIntroIndex] = useState("");
  const [visibility, setVisibility] = useState("PUBLIC");
  const navigate = useNavigate();
  const fileInputRef = useRef(null);

  useEffect(() => {
    if (currentProfile) {
      setImagePreview(currentProfile.profileImageUrl || "/default-profile.png");
      setIntroIndex(currentProfile.message || "");
      setVisibility(currentProfile.visibility || "PUBLIC");
      setNickName(currentProfile.nickName || "");
    }
  }, [currentProfile]);

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setNewImage(file);
      setImagePreview(URL.createObjectURL(file));
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({ image: newImage, introIndex, visibility, nickName });
    onRequestClose();
  };

  const handleDeleteAccount = async () => {
    const confirmed = window.confirm("정말로 회원탈퇴 하시겠습니까?");
    if (!confirmed) return;

    try {
      const token = sessionStorage.getItem("accessToken");
      console.log(token);
      await axios.delete("http://localhost:8080/users/me", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      sessionStorage.removeItem("accessToken");
      alert("회원탈퇴가 완료되었습니다.");
      navigate("/");
    } catch (error) {
      console.error("회원탈퇴 실패:", error);
      alert("회원탈퇴에 실패했습니다.");
    }
  };

  return (
      <Modal
          isOpen={isOpen}
          onRequestClose={onRequestClose}
          contentLabel="프로필 설정"
          className="profile-modal"
          overlayClassName="overlay"
      >
        <h2>프로필 설정</h2>
        <form onSubmit={handleSubmit} className="profile-form">
          <div className="profile-row">
            <div className="left-section">
              <div
                  className="profile-image-wrapper"
                  onClick={() => fileInputRef.current.click()}
              >
                <img src={imagePreview} alt="프로필" className="profile-image" />
                <div className="gear-icon">
                  <FaCog />
                </div>
                <input
                    type="file"
                    accept="image/*"
                    ref={fileInputRef}
                    onChange={handleImageChange}
                    style={{ display: "none" }}
                />
              </div>
            </div>

            <div className="right-section">
              <label>
                <input
                    type="text"
                    value={nickName}
                    onChange={(e) => setNickName(e.target.value)}
                    placeholder="닉네임"
                    className="nickname-input"
                />
              </label>

              <label>
                <input
                    type="text"
                    value={introIndex}
                    onChange={(e) => setIntroIndex(e.target.value)}
                    placeholder="한줄 메세지"
                    className="message-input"
                />
              </label>

              <div className="visibility">
                <div>공개 범위</div>
                <label>
                  <input
                      type="radio"
                      value="PUBLIC"
                      checked={visibility === "PUBLIC"}
                      onChange={(e) => setVisibility(e.target.value)}
                  />
                  전체공개
                </label>
                <label>
                  <input
                      type="radio"
                      value="FRIENDS"
                      checked={visibility === "FRIENDS"}
                      onChange={(e) => setVisibility(e.target.value)}
                  />
                  친구공개
                </label>
                <label>
                  <input
                      type="radio"
                      value="PRIVATE"
                      checked={visibility === "PRIVATE"}
                      onChange={(e) => setVisibility(e.target.value)}
                  />
                  비공개
                </label>
              </div>
            </div>
          </div>

          <div className="modal-buttons">
            <button
                type="button"
                className="delete-account"
                onClick={handleDeleteAccount}
            >
              회원탈퇴
            </button>
            <button type="submit">저장</button>
            <button type="button" className="cancel" onClick={onRequestClose}>
              취소
            </button>
          </div>
        </form>
      </Modal>
  );
};

export default SetProfileModal;

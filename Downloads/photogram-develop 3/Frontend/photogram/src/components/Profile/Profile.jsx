import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./Profile.css";
import profileData from "../../assets/data/profileData.json"; // ✅ JSON 파일 import

const Profile = () => {
  const [searchTerm, setSearchTerm] = useState("");
  const [filteredFriends, setFilteredFriends] = useState(profileData.friends);
  const navigate = useNavigate();

  // 검색어가 바뀔 때마다 친구 목록 필터링
  useEffect(() => {
    const filtered = profileData.friends.filter((friend) =>
      friend.username.includes(searchTerm)
    );
    setFilteredFriends(filtered);
  }, [searchTerm]);

  // 클릭 시 마이페이지로 이동하는 함수
  const goToMyPage = () => {
    navigate("/mypage");
  };

  return (
    <div className="profile-panel">
      {/* 고정된 상단 프로필 영역 */}
      <div className="profile-header">
        <img
          className="profile-main-image"
          src={profileData.me.image}
          alt={profileData.me.username}
          onClick={goToMyPage} // 이미지 클릭 시 이동
          style={{ cursor: "pointer" }}
        />
        <div>
          <div className="profile-username" onClick={goToMyPage} style={{ cursor: "pointer" }}>
            {profileData.me.username}
          </div>
        </div>
      </div>

      {/* 검색 영역과 친구 목록 제목 */}
      <div className="friend-fixed-header">
        <div className="friend-section-header">친구 목록</div>
        <input
          type="text"
          className="friend-search-input"
          placeholder="친구 이름 검색..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
      </div>

      {/* 스크롤 가능한 친구 목록 */}
      <div className="friend-scroll-list">
        {filteredFriends.map((friend, index) => (
          <div className="friend-row" key={index}>
            <img
              className="friend-avatar"
              src={friend.image}
              alt={friend.username}
            />
            <div className="friend-info">
              <div className="friend-username">{friend.username}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Profile;
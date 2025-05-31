import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./MPProfile.css";
import { FaCog } from "react-icons/fa";
import SetProfileModal from "./SetProfileModal";
import axios from "axios";

const MPProfile = ({ isMainPage, userData, isMyPage }) => {
  console.log("✅ [MPProfile 바로 진입 시] userData:", userData);
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("friends");
  const [isExpanded, setIsExpanded] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [searchAddTerm, setSearchAddTerm] = useState("");
  const [requestedUsers, setRequestedUsers] = useState({});
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [profileData, setProfileData] = useState(null);
  const [filteredFriends, setFilteredFriends] = useState([]);
  const [filteredNewUsers, setFilteredNewUsers] = useState([]);
  const [friendRequests, setFriendRequests] = useState([]);

  const openModalWithProfile = async () => {
    try {
      const token = sessionStorage.getItem("accessToken"); // JWT 토큰이 있다면
      const response = await axios.get("http://localhost:8080/users/me", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      // API 응답 데이터 예시 기반으로 수정하세요
      const { profileImageUrl, nickname, message, visibility } = response.data;

      setProfileData({
        profileImageUrl,
        nickname,
        message,
        visibility,
      });

      setIsModalOpen(true);
    } catch (error) {
      console.error("프로필 정보 가져오기 실패:", error);
      // 에러 핸들링 추가 가능
    }
  };

  const handleProfileSubmit = async ({
    nickname,
    image,
    message,
    visibility,
  }) => {
    try {
      const token = sessionStorage.getItem("accessToken");

      const formData = new FormData();

      // ✅ JSON 객체를 Blob으로 만들어 updateRequest로 전송
      const updateRequest = {
        nickname,
        message,
        visibility,
      };
      const jsonBlob = new Blob([JSON.stringify(updateRequest)], {
        type: "application/json",
      });
      formData.append("updateRequest", jsonBlob);

      // ✅ 이미지 파일이 있다면 첨부
      if (image) {
        formData.append("profileImage", image);
      }

      await axios.patch("http://localhost:8080/users/me", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
          Authorization: `Bearer ${token}`,
        },
      });

      alert("프로필이 성공적으로 수정되었습니다.");
    } catch (error) {
      console.error("프로필 수정 실패:", error);
      alert("프로필 수정에 실패했습니다.");
    }
  };

  useEffect(() => {
    // 친구 목록 탭이 열릴 때 API 요청
    const fetchFriends = async () => {
      if (isExpanded && activeTab === "friends" && userData) {
        try {
          const token = sessionStorage.getItem("accessToken");
          console.log("✅ accessToken:", token); // 확인용

          const res = await fetch("http://localhost:8080/friends/search?accepted=true", {
            headers: {
              Authorization: `Bearer ${token}`
            },
          });
          const data = await res.json();
          
          console.log("✅ 친구 목록 API 응답:", data); // 데이터 확인용

          setFilteredFriends(data); // 받은 친구 목록 전체 저장
        } catch (err) {
          console.error("친구 목록 불러오기 실패:", err);
        }
      }
    };

    fetchFriends();
  }, [isExpanded, activeTab, userData]);

  useEffect(() => {
    if (userData?.newUsers) {
      const filtered =
        searchAddTerm.trim() === ""
          ? []
          : userData.newUsers.filter((user) =>
              user.username.includes(searchAddTerm)
            );
      setFilteredNewUsers(filtered);
    }
  }, [searchAddTerm, userData]);

  const goToUserPage = (nickname) => {
    navigate(`/${nickname}`);
  };

  const handleTabClick = (tabName) => {
    if (activeTab === tabName) {
      setIsExpanded((prev) => !prev);
    } else {
      setActiveTab(tabName);
      setIsExpanded(true);
    }
  };

  const handleSearchClick = async () => {
    if (searchAddTerm.trim() === "") {
      setFilteredNewUsers([]);
      return;
    }

    try {
      const token = sessionStorage.getItem("accessToken");
      const res = await fetch(`http://localhost:8080/users/search?nickname=${searchAddTerm}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        setFilteredNewUsers([]); // 잘못된 경우도 제거
        return;
      }

      const data = await res.json();

      console.log("✅ data:", data);

      if (Array.isArray(data)) {
        console.log("✅ 배열로 받은 유저:", data);
        setFilteredNewUsers(data); // 바로 배열로 넣기
      } else {
        console.log("❌ 유저 없음 또는 형식 오류:", data);
        setFilteredNewUsers([]);
      }      
    } catch (err) {
      console.error("친구 검색 실패:", err);
      setFilteredNewUsers([]);
    }
  };

  useEffect(() => {
    console.log("✅ [렌더링 직전] filteredNewUsers:", filteredNewUsers);
  }, [filteredNewUsers]);  

  useEffect(() => {
    const fetchRequests = async () => {
      if (isExpanded && activeTab === "requests") {
        try {
          const token = sessionStorage.getItem("accessToken");
          const res = await fetch("http://localhost:8080/friends/search?accepted=false", {
            headers: { Authorization: `Bearer ${token}` },
          });
          const data = await res.json();
          setFriendRequests(data);
        } catch (err) {
          console.error("친구 요청 목록 가져오기 실패:", err);
        }
      }
    };

    fetchRequests();
  }, [isExpanded, activeTab]);

  const handleFriendRequestToggle = async (receiverId, nickName) => {
    const token = sessionStorage.getItem("accessToken");

    const isRequested = requestedUsers[receiverId];

    if (!isRequested) {
      // 요청 보내기
      try {
        const res = await fetch(`http://localhost:8080/friends/request`, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ receiverId }),
        });

        if (res.ok) {
          setRequestedUsers((prev) => ({ ...prev, [receiverId]: true }));
        } else {
          alert("친구 요청 실패");
        }
      } catch (err) {
        console.error("친구 요청 중 에러:", err);
      }
    } else {
      // 요청 취소
      try {
        const res = await fetch(`http://localhost:8080/friends/request?nickName=${nickName}`, {
          method: "DELETE",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (res.ok) {
          setRequestedUsers((prev) => ({ ...prev, [receiverId]: false }));
        } else {
          alert("요청 취소 실패");
        }
      } catch (err) {
        console.error("친구 요청 취소 중 에러:", err);
      }
    }
  };

  const handleAccept = async (nickname) => {
    try {
      const token = sessionStorage.getItem("accessToken");
      await fetch(`http://localhost:8080/friends/request?nickName=${nickname}`, {
        method: "PATCH",
        headers: { Authorization: `Bearer ${token}` },
      });
      // 목록 갱신
      setFriendRequests((prev) => prev.filter((r) => r.nickName !== nickname)); // 수락 버튼 누르면 알림 삭제
    } catch (err) {
      console.error("수락 실패:", err);
    }
  };

  const handleReject = async (nickname) => {
    try {
      const token = sessionStorage.getItem("accessToken");
      await fetch(`http://localhost:8080/friends/request?nickName=${nickname}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });
      setFriendRequests((prev) => prev.filter((r) => r.nickName !== nickname));
    } catch (err) {
      console.error("거절 실패:", err);
    }
  };

  return (
    <div className="mp-profile-panel">
      {/* 프로필 상단 */}
      {userData && (
        <div className="mp-profile-header">
          <img
            className="mp-profile-main-image"
            src={userData.profileImageUrl}
            alt={userData.nickName}
            onClick={() => goToUserPage(userData.nickName)}
            style={{ cursor: "pointer" }}
          />
          <div className="mp-profile-info-group">
            <div
              className="mp-profile-username"
              onClick={() => goToUserPage(userData.nickName)}
              style={{ cursor: "pointer" }}
            >
              {userData.nickName}
            </div>
            <div className="mp-profile-meta">
              <div className="mp-meta-box">
                <span className="mp-meta-label">게시물</span>
                <span className="mp-meta-value">{userData.posts ?? 0}</span>
              </div>
              <div className="mp-meta-box">
                <span className="mp-meta-label">친구</span>
                <span className="mp-meta-value">{userData.friends?.length ?? 0}</span>
              </div>
            </div>
            <div className="mp-profile-realname">{userData.name}</div>
            <div className="mp-profile-introindex">{userData.introIndex}</div>
          </div>
          {(isMyPage || isMainPage) && (
            <button className="mp-settings-btn" onClick={openModalWithProfile}>
              <FaCog />
            </button>
          )}
          <SetProfileModal
            isOpen={isModalOpen}
            onRequestClose={() => setIsModalOpen(false)}
            onSubmit={handleProfileSubmit}
            currentProfile={profileData}
          />
        </div>
      )}

      {/* 탭 버튼 */}
      <div className="mp-friend-tab-buttons">
        <button
          className={`mp-friend-tab-button ${
            isExpanded && activeTab === "friends" ? "active" : ""
          }`}
          onClick={() => handleTabClick("friends")}
        >
          친구 목록
        </button>
        <button
          className={`mp-friend-tab-button ${
            isExpanded && activeTab === "add" ? "active" : ""
          }`}
          onClick={() => handleTabClick("add")}
        >
          친구 요청
        </button>
        <button
          className={`mp-friend-tab-button ${
            isExpanded && activeTab === "requests" ? "active" : ""
          }`}
          onClick={() => handleTabClick("requests")}
        >
          친구 수락
        </button>
      </div>
      {/* ▼ 아래 영역은 탭이 펼쳐졌을 때만 렌더링 */}
      {/* 친구 목록 */}
      {isExpanded && activeTab === "friends" && (
        <>
          <input
            type="text"
            className="mp-friend-search-list-input"
            placeholder="Search Friends..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                handleSearchClick();
              }
            }}
          />
          <div className="mp-friend-scroll-list">
            {filteredFriends
              .filter((friend) => friend.nickName.includes(searchTerm))
              .map((friend, index) => (
                <div className="mp-friend-row" key={index}>
                  <img
                    className="mp-friend-avatar"
                    src={friend.profileImageUrl}
                    alt={friend.nickName}
                    onClick={() => goToUserPage(friend.nickName)}
                    style={{ cursor: "pointer" }}
                  />
                  <div className="mp-friend-info">
                    <div className="mp-friend-username-group">
                      <div
                        className="mp-friend-username"
                        onClick={() => goToUserPage(friend.nickName)}
                        style={{ cursor: "pointer" }}
                      >
                        {friend.nickName}
                      </div>
                      <div className="mp-friend-realname">{friend.name}</div>
                    </div>
                  </div>
                </div>
            ))}
          </div>
        </>
      )}

      {/* 친구 요청 */}
      {isExpanded && activeTab === "add" && (
        <div className="mp-friend-add-section">
          <div className="mp-search-bar-wrapper">
            <input
              type="text"
              className="mp-friend-search-input"
              placeholder="Search new Friends..."
              value={searchAddTerm}
              onChange={(e) => setSearchAddTerm(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  handleSearchClick();
                }
              }}
            />
            <button className="mp-search-btn" onClick={handleSearchClick}>
              🔍
            </button>
          </div>

          <div className="mp-friend-add-list">
            {filteredNewUsers
              .filter(
                (user) =>{
                  // 🧪 테스트 로그 추가 위치
                  console.log("🧪 검사 중 유저 nickName:", user.nickName);
                  console.log("현재 내 nickName:", userData?.nickName);
                  console.log("내 친구 목록:", userData?.friends?.map((f) => f.nickName));

                  const isMe = user.nickName === userData?.nickName;

                  const friendList = userData?.friends || []; // 친구 목록이 없으면 빈 배열로
                  const isAlreadyFriend = friendList.some( // 친구 안 뜨게 수정해야됨
                    (f) => f.nickName === user.nickName
                  );

                  return !isMe && !isAlreadyFriend;
                })
              .map((user, index) => (
                <div className="mp-add-row" key={index}>
                  <img
                    className="mp-add-avatar"
                    src={user.profileImageUrl}
                    alt={user.nickName}
                    onClick={() => goToUserPage(user.nickName)}
                    style={{ cursor: "pointer" }}
                  />
                  <div className="mp-add-info">
                    <div className="mp-add-username-group">
                      <div 
                        className="mp-add-username"
                        onClick={() => goToUserPage(user.nickName)}
                        style={{ cursor: "pointer" }}
                      >
                        {user.nickName}</div>
                        {/* 닉네임 */}
                      <div className="mp-add-realname">{user.name}</div>
                      {/* 실제 이름 */}
                    </div>
                  </div>
                  <button
                    className="mp-add-btn"
                    onClick={() => handleFriendRequestToggle(user.memberId, user.nickName)}
                  >
                    {requestedUsers[user.memberId] ? "요청됨" : "요청"}
                  </button>
                </div>
            ))}
          </div>
        </div>
      )}

      {/* 친구 수락 */}
      {isExpanded && activeTab === "requests" && (
        <div className="mp-friend-request-list">
          {friendRequests.map((req, index) => (
            <div className="mp-friend-request-card" key={index}>
              {/* 거절 버튼 */}
              <button
                className="mp-close-btn"
                onClick={() => handleReject(req.nickName)}
              >
                ×
              </button>

              {/* 프로필 이미지 */}
              <img
                className="mp-request-avatar"
                src={req.profileImageUrl}
                alt={req.nickName}
                onClick={() => goToUserPage(req.nickName)}
                style={{ cursor: "pointer" }}
              />

              {/* 메시지 */}
              <div className="mp-request-message">
                <strong onClick={() => goToUserPage(req.nickName)} style={{ cursor: "pointer" }}>{req.nickName}</strong>님이 친구 요청을 보냈습니다.
              </div>

              {/* 수락 버튼 */}
              <button
                className="mp-accept-btn"
                onClick={() => handleAccept(req.nickName)}
              >
                수락
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default MPProfile;
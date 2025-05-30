import React, { useState } from "react";
import { useParams } from "react-router-dom";
import ReactModal from "react-modal";
import SeoulMap from "../components/MainContent/SeoulMap";
import MPProfile from "../components/Profile/MPProfile";
import "./MyPage.css";
import "./MainPage.css";

const MyPage = ({ currentUser }) => {
  const { nickname: routeNickname } = useParams();
  const isMyPage = currentUser?.nickname === routeNickname;
  const [searchQuery, setSearchQuery] = useState("");
  const [showHashtags, setShowHashtags] = useState(false);
  const [popularHashtags] = useState([
    "#강남구", "#홍대", "#서울여행", "#맛집", "#봄여행", "#여행스타그램",
    "#서울맛집", "#강남맛집", "#서울데이트", "#봄꽃"
  ]);
  const [selectedArea, setSelectedArea] = useState("");
  const [areaPosts, setAreaPosts] = useState({});
  const [selectedPost, setSelectedPost] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isWriteModalOpen, setIsWriteModalOpen] = useState(false);
  const [image, setImage] = useState([]);
  const [uploadedImageUrls, setUploadedImageUrls] = useState([]);
  const [text, setText] = useState("");
  const [step, setStep] = useState(1);


  const openModal = async (areaName) => {
    setSelectedArea(areaName);
    setIsModalOpen(true);

    try {
      const token = sessionStorage.getItem("token");
      const nicknameToUse = isMyPage ? currentUser.nickname : routeNickname;

      const response = await fetch(
        `http://localhost:8080/posts/userpage?area=${encodeURIComponent(areaName)}&nickname=${nicknameToUse}`,
        {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (response.ok) {
        const data = await response.json();
        setAreaPosts((prev) => ({ ...prev, [areaName]: data }));
      } else {
        alert("게시물 조회 실패");
      }
    } catch (error) {
      console.error("구별 게시물 조회 실패:", error);
      alert("네트워크 오류");
    }
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setSelectedArea("");
  };

  const handleImageClick = (post) => {
    setSelectedPost(post);
  };

  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
    setShowHashtags(e.target.value.length > 0);
  };

  const handleHashtagClick = (hashtag) => {
    setSearchQuery(hashtag);
    setShowHashtags(false);
  };

  const openWriteModal = () => setIsWriteModalOpen(true);
  const closeWriteModal = () => setIsWriteModalOpen(false);

  const handleImageUpload = (e) => {
    const files = Array.from(e.target.files);
    setImage(prev => [...prev, ...files]);
  };

  const handleImageUploadSubmit = async () => {
    if (image.length === 0) return;
    const formData = new FormData();
    image.forEach((file) => formData.append("imageFiles", file));

    try {
      const response = await fetch("http://localhost:8080/images/upload", {
        method: "POST",
        body: formData,
      });

      if (response.ok) {
        const urls = await response.json();
        setUploadedImageUrls(urls);
        setStep(2);
      } else {
        alert("이미지 업로드 실패");
      }
    } catch (err) {
      console.error("업로드 에러:", err);
      alert("네트워크 오류");
    }
  };

  const handleTextChange = (e) => setText(e.target.value);

  const handlePostSubmit = async () => {
    if (uploadedImageUrls.length === 0 && !text) return;

    const postData = {
      text,
      area: selectedArea,
      imageUrls: uploadedImageUrls,
    };

    try {
      const response = await fetch("http://localhost:8080/posts", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(postData),
      });

      if (response.ok) {
        alert("게시글이 성공적으로 작성되었습니다!");
        setText("");
        setImage([]);
        setUploadedImageUrls([]);
        setIsWriteModalOpen(false);
        setStep(1);
      } else {
        console.error("업로드 실패", await response.text());
        alert("작성 실패");
      }
    } catch (err) {
      console.error("에러 발생:", err);
      alert("네트워크 오류");
    }
  };

  return (
    <div className="mypage">
      <MPProfile isMainPage={false} userData={currentUser} isMyPage={isMyPage} />

      <div className="mypage-map-container">
        <SeoulMap onAreaClick={openModal} />
      </div>

      <div className="mypage-search-bar-container">
        <input
          type="text"
          placeholder="search hashtag..."
          value={searchQuery}
          onChange={handleSearchChange}
          className="mypage-search-input"
        />
        <span className="mypage-search-icon">🔍</span>
        {showHashtags && (
          <div className="mypage-hashtag-suggestions">
            {popularHashtags.map((hashtag, index) => (
              <div key={index} className="mypage-hashtag" onClick={() => handleHashtagClick(hashtag)}>
                {hashtag}
              </div>
            ))}
          </div>
        )}
      </div>

      <ReactModal isOpen={isModalOpen} onRequestClose={closeModal} className="modal" overlayClassName="overlay">
        <h2>{selectedArea}</h2>
        <div className="modal-images-container">
          <div className="row-top">
            {(areaPosts[selectedArea] || []).flatMap((post, idx) =>
              post.images.slice(0, 5).map((img, i) => (
                <div key={`${idx}-top-${i}`} className="modal-filled" onClick={() => handleImageClick(post)}>
                  <img src={img} alt="uploaded" style={{ width: "100%", borderRadius: "8px" }} />
                </div>
              ))
            )}
          </div>
          <div className="row-bottom">
            {(areaPosts[selectedArea] || []).flatMap((post, idx) =>
              post.images.slice(5).map((img, i) => (
                <div key={`${idx}-bottom-${i}`} className="modal-filled" onClick={() => handleImageClick(post)}>
                  <img src={img} alt="uploaded" style={{ width: "100%", borderRadius: "8px" }} />
                </div>
              ))
            )}
          </div>
        </div>
        {isMyPage && (
          <button className="write-button" onClick={openWriteModal}>글쓰기</button>
        )}
        <button onClick={closeModal}>닫기</button>
      </ReactModal>

      {isWriteModalOpen && (
        <ReactModal isOpen={true} onRequestClose={closeWriteModal} className="modal" overlayClassName="overlay">
          <h2>글 작성하기</h2>
          {step === 1 ? (
            <>
              <input type="file" accept="image/*" multiple onChange={handleImageUpload} />
              <div style={{ display: 'flex', flexWrap: 'wrap', marginTop: '10px' }}>
                {image.map((img, idx) => (
                  <img
                    key={idx}
                    src={typeof img === 'string' ? img : URL.createObjectURL(img)}
                    alt={`preview-${idx}`}
                    style={{ width: '120px', margin: '5px', borderRadius: '8px' }}
                  />
                ))}
              </div>
              <div className="write-section">
                <button onClick={handleImageUploadSubmit}>다음</button>
                <button onClick={closeWriteModal}>취소</button>
              </div>
            </>
          ) : (
            <>
              <textarea
                rows="4"
                placeholder="글을 작성하세요..."
                value={text}
                onChange={handleTextChange}
              ></textarea>
              <div className="write-section">
                <button className="write-complete-button" onClick={handlePostSubmit}>작성 완료</button>
                <button onClick={() => setStep(1)}>이전</button>
              </div>
            </>
          )}
        </ReactModal>
      )}

      {selectedPost && (
        <ReactModal isOpen={true} onRequestClose={() => setSelectedPost(null)} className="modal" overlayClassName="overlay">
          <h2>게시글</h2>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px' }}>
            {selectedPost.images.map((img, idx) => (
              <img key={idx} src={img} alt={`post-img-${idx}`} style={{ width: '550px', borderRadius: '8px' }} />
            ))}
          </div>
          <p style={{ marginTop: '20px' }}>{selectedPost.text}</p>
          <button onClick={() => setSelectedPost(null)}>닫기</button>
        </ReactModal>
      )}
    </div>
  );
};

export default MyPage;
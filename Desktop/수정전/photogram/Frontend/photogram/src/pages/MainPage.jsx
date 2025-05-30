import React, { useState, useMemo, useEffect } from "react";
import ReactModal from "react-modal";
import { FiSearch, FiX, FiHeart, FiMessageCircle, FiEdit2, FiTrash2, FiSend, FiCornerDownRight, FiChevronLeft, FiChevronRight } from "react-icons/fi";
import SeoulMap from "../components/MainContent/SeoulMap";
import MPProfile from "../components/Profile/MPProfile";
import "./MainPage.css";
import HashtagBubbles from '../components/HashtagBubbles';
import axios from "axios";

ReactModal.setAppElement('#root');

const MainPage = ({ isLogined, setIsLogined }) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [step, setStep] = useState(1); // 1: 이미지 업로드, 2: 텍스트 입력
  const [uploadedImageUrls, setUploadedImageUrls] = useState([]); // S3 업로드 후 받은 URL 저장
  const [selectedArea, setSelectedArea] = useState("");
  const [isWriteModalOpen, setIsWriteModalOpen] = useState(false);
  const [image, setImage] = useState([]);
  const [isEditMode, setIsEditMode] = useState(false);
  const [editingPostId, setEditingPostId] = useState(null); // 수정할 게시글 ID
  const [text, setText] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [showHashtags, setShowHashtags] = useState(false);
  const [isNightTheme, setIsNightTheme] = useState(false);
  const [selectedPost, setSelectedPost] = useState(null);
  const [areaPosts, setAreaPosts] = useState({});
  const [replyTarget, setReplyTarget] = useState(null);
// ex) { postId: 123, commentId: 456 } 형태로 대댓글 대상 기억
  const [likedPosts, setLikedPosts] = useState(new Set()); // 좋아요 상태 저장
  const [likeCounts, setLikeCounts] = useState({}); // 게시글별 좋아요 수 저장
  const [comments, setComments] = useState({}); // { postId: [ {id, text}, ... ] }
  const [commentText, setCommentText] = useState(""); // 댓글 입력창용
  const [editingCommentId, setEditingCommentId] = useState(null); // 수정 중인 댓글 ID
  const [activeCommentPost, setActiveCommentPost] = useState(null);
  const [isHashtagModalOpen, setIsHashtagModalOpen] = useState(false);
  const [recommendedTags, setRecommendedTags] = useState([]);
  const [selectedTags, setSelectedTags] = useState([]);
  const [popularHashtags, setPopularHashtags] = useState([
    "#강남구", "#홍대", "#서울여행", "#맛집", "#봄여행", "#여행스타그램",
    "#서울맛집", "#강남맛집", "#서울데이트", "#봄꽃"
  ]);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [isTopBarVisible, setIsTopBarVisible] = useState(true);
  const [highlightedGus, setHighlightedGus] = useState([]); // 하이라이트된 구들을 저장하는 상태 추가
  const [userData, setUserData] = useState(null);
  const [isLoadingComments, setIsLoadingComments] = useState(false); // 댓글 로딩 상태 추가

  // JWT 토큰 관련 상태 추가
  const [token, setToken] = useState(sessionStorage.getItem('accessToken'));

  useEffect(() => {
    // 토큰 변경 시 localStorage 업데이트
    if (token) {
      sessionStorage.setItem('accessToken', token);
    }
  }, [token]);

  useEffect(() => {
    const token = sessionStorage.getItem("accessToken");
    if (!token) {
      setUserData(null);
      return;
    }
  
    fetch("http://localhost:8080/users/me", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => {
        if (!res.ok) throw new Error("인증 실패");
        return res.json();
      })
      .then((data) => {
        console.log("불러온 userData:", data);
        setUserData(data)
      })
      .catch((err) => {
        console.error("유저 정보 불러오기 실패:", err);
        sessionStorage.removeItem("accessToken");
        setUserData(null);
      });
  }, []);
  
  const [modalSearchQuery, setModalSearchQuery] = useState(""); // 모달 내 검색어 상태 추가
  const [filteredPosts, setFilteredPosts] = useState(null); // 필터링된 게시물 상태 추가

  // 인증 헤더 생성 함수
  const getAuthHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  });

  const toggleTheme = () => setIsNightTheme((prev) => !prev);
  // const openWriteModal = () => setIsWriteModalOpen(true); // 기존 코드 주석 처리
  const closeWriteModal = () => {
    // resetForm(); // closeWriteModal 호출 시 resetForm을 사용해도 좋음
    setIsWriteModalOpen(false);
    // 폼 상태를 여기서 초기화하거나, 열릴 때 초기화하도록 보장
    // resetForm()을 호출하면 isEditMode 등도 초기화되므로, 단순 닫기만 할지 결정 필요
    // 여기서는 닫기만 하고, 열 때 초기화하도록 변경
  };

  // 새 글 작성 모달 열기 함수 수정
  const openWriteModal = () => {
    setIsEditMode(false);       // 수정 모드 해제
    setEditingPostId(null);     // 수정 중인 게시글 ID 초기화
    setText("");                 // 텍스트 내용 초기화
    setImage([]);               // 선택된 파일 목록 초기화
    setUploadedImageUrls([]);   // 업로드된 (또는 될) URL 목록 초기화
    setStep(1);                 // 새 글 작성은 1단계(이미지 업로드)부터 시작
    setSelectedTags([]);        // 선택된 해시태그 초기화
    // isHashtagModalOpen은 AI 해시태그 추천 모달이므로, 글쓰기 시작 시점에는 false가 적절할 수 있음
    setIsHashtagModalOpen(false); 

    setIsWriteModalOpen(true);  // 글쓰기 모달 열기
  };

  // 구 이름 매핑 추가
  const guNameMapping = {
    'gangnam': '강남구',
    'gangnamgu': '강남구',
    'seocho': '서초구',
    'seochogu': '서초구',
    'songpa': '송파구',
    'songpagu': '송파구',
    'mapo': '마포구',
    'mapogu': '마포구',
    'yongsan': '용산구',
    'yongsangu': '용산구',
    'jongno': '종로구',
    'jongrogu': '종로구',
    'jung': '중구',
    'junggu': '중구',
    'yongdeungpo': '영등포구',
    'yongdeungpogu': '영등포구',
    'gangdong': '강동구',
    'gangdonggu': '강동구',
    'gangseo': '강서구',
    'gangseogu': '강서구',
    'gwanak': '관악구',
    'gwanakgu': '관악구',
    'gwangjin': '광진구',
    'gwangjingu': '광진구',
    'guro': '구로구',
    'gurogu': '구로구',
    'geumcheon': '금천구',
    'geumcheongu': '금천구',
    'nowon': '노원구',
    'nowongu': '노원구',
    'dobong': '도봉구',
    'dobonggu': '도봉구',
    'dongdaemun': '동대문구',
    'dongdaemungu': '동대문구',
    'dongjak': '동작구',
    'dongjakgu': '동작구',
    'seodaemun': '서대문구',
    'seodaemungu': '서대문구',
    'seongbuk': '성북구',
    'seongbukgu': '성북구',
    'seongdong': '성동구',
    'seongdonggu': '성동구',
    'yangcheon': '양천구',
    'yangcheongu': '양천구',
    'eunpyeong': '은평구',
    'eunpyeonggu': '은평구',
    'jungrang': '중랑구',
    'jungranggu': '중랑구'
  };

  const getGuName = (searchText) => {
    if (!searchText) return null;

    const normalized = searchText.toLowerCase().replace(/\s+/g, '');

    // 한글 검색
    if (/[가-힣]/.test(normalized)) {
      const guName = Object.values(guNameMapping).find(name =>
          name.replace(/\s+/g, '').includes(normalized)
      );
      return guName || null;
    }

    // 영문 검색
    for (const [eng, kor] of Object.entries(guNameMapping)) {
      if (eng.includes(normalized)) {
        return kor;
      }
    }
    return null;
  };

  // 모달 내 해시태그 검색 처리 함수
  const handleModalSearch = async (e) => {
    const value = e.target.value;
    setModalSearchQuery(value);

    if (!value) {
      setFilteredPosts(null);
      return;
    }

    if (value.startsWith('#')) {
      // 임시: 프론트엔드에서 필터링
      const searchTag = value.toLowerCase().slice(1);
      const currentAreaPosts = areaPosts[selectedArea] || [];
      const filtered = currentAreaPosts.filter(post =>
              post.postImageHashtags && post.postImageHashtags.some(tag =>
                  tag.hashtag.name.toLowerCase().includes(searchTag)
              )
      );
      setFilteredPosts(filtered);
    }
  };

  // 모달 내 검색 엔터 처리
  const handleModalSearchKeyPress = (e) => {
    if (e.key === 'Enter' && modalSearchQuery.startsWith('#')) {
      handleModalSearch(e);
    }
  };

  // 메인 화면 해시태그 검색 시 구 하이라이트를 위한 매핑 데이터
  const hashtagToGusMapping = {
    "#강남맛집": ["강남"],
    "#서울맛집": ["강남", "마포", "종로"],
    "#홍대": ["마포"],
    "#서울여행": ["종로", "강남", "용산"],
    "#카페스타그램": ["강남", "마포", "서초"],
    "#데이트": ["마포", "강남", "홍대"],
    "#카페": ["마포", "강남", "서초", "종로"],
    "#카페투어": ["마포", "강남", "서초"],
    "#경복궁": ["종로"],
    "#인사동": ["종로"],
    "#전통찻집": ["종로"],
    "#연남동": ["마포"],
    "#홍대맛집": ["마포"],
    "#강남역": ["강남"],
    "#강남카페": ["강남"],
    "#분위기맛집": ["강남", "마포"],
    "#야간개장": ["종로"]
  };

  const handleHashtagClick = async (hashtag) => {
    // 이미 #으로 시작하는 경우 그대로 사용, 아니면 #을 추가
    const hashtagWithSymbol = hashtag.startsWith('#') ? hashtag : `#${hashtag}`;
    setSearchQuery(hashtagWithSymbol);
    setShowHashtags(false);

    // 임시: 매핑 데이터 사용
    const gus = hashtagToGusMapping[hashtagWithSymbol] || [];
    if (gus.length > 0) {
      setHighlightedGus(gus);
      console.log(`${hashtagWithSymbol} 검색 결과:`, gus);
    } else {
      console.log(`${hashtagWithSymbol}에 해당하는 구가 없습니다.`);
      setHighlightedGus([]);
    }
  };

  // 구 검색 엔터 이벤트 핸들러 수정
  const handleSearchKeyPress = (e) => {
    if (e.key === 'Enter') {
      if (searchQuery.startsWith('#')) {
        const gus = hashtagToGusMapping[searchQuery] || [];
        if (gus.length > 0) {
          setHighlightedGus(gus);
          console.log(`${searchQuery} 검색 결과:`, gus);
        } else {
          console.log(`${searchQuery}에 해당하는 구가 없습니다.`);
          setHighlightedGus([]);
        }
        setShowHashtags(false);
      } else {
        // 구 검색일 경우 기존 로직 유지
        const guName = getGuName(searchQuery);
        if (guName) {
          openModal(guName);
          setSearchQuery('');
          setShowHashtags(false);
        }
      }
    }
  };

  const handleSearchChange = async (e) => {
    const value = e.target.value;
    setSearchQuery(value);

    if (value.startsWith('#')) {
      setShowHashtags(true);
      // 해시태그 검색어에 해당하는 구 찾기
      const searchTag = value.toLowerCase();
      const matchingGus = [];

      // 해시태그 매핑 데이터에서 검색
      Object.entries(hashtagToGusMapping).forEach(([hashtag, gus]) => {
        if (hashtag.toLowerCase().includes(searchTag)) {
          matchingGus.push(...gus);
        }
      });

      // 중복 제거 후 하이라이트 설정
      setHighlightedGus([...new Set(matchingGus)]);

      try {
        const tags = await fetchPopularHashtags();
        setPopularHashtags(tags);
      } catch (error) {
        console.error('해시태그 가져오기 실패:', error);
        setPopularHashtags([]); // 에러 시 빈 배열로 설정
      }
    } else {
      setShowHashtags(false);
      setPopularHashtags([]);

      // 구 검색 로직
      const guName = getGuName(value);
      if (guName) {
        const highlightName = guName.replace(/구$/, '');
        setHighlightedGus([highlightName]);
      } else {
        setHighlightedGus([]);
      }
    }
  };

  const openModal = async (areaName) => {
    setSelectedArea(areaName);
    setIsModalOpen(true);
    setIsTopBarVisible(false);
    // setLoading(true) // 필요하다면 로딩 상태 추가

    try {
      const response = await axios.get(`http://localhost:8080/histories?area=${encodeURIComponent(areaName)}`, {
        headers: getAuthHeaders()
      });
      const postsData = response.data; // postsData는 PostResponse[] 형태일 것으로 기대

      setAreaPosts((prev) => ({
        ...prev,
        [areaName]: postsData
      }));

      // --- 목록 내 게시물들의 좋아요 상태 및 카운트 초기화 ---
      if (postsData && postsData.length > 0) {
        let newLikedPosts = new Set(likedPosts);
        let newLikeCounts = { ...likeCounts };

        postsData.forEach(p => {
          if (typeof p.isLiked === 'boolean') { // 백엔드에서 isLiked 필드를 준다고 가정
            if (p.isLiked) {
              newLikedPosts.add(p.id);
            } else {
              newLikedPosts.delete(p.id); // 혹시 몰라 삭제 로직도 추가 (일관성)
            }
          }
          if (typeof p.likeCount === 'number') { // 백엔드에서 likeCount 필드를 준다고 가정
            newLikeCounts[p.id] = p.likeCount;
          }
        });
        setLikedPosts(newLikedPosts);
        setLikeCounts(newLikeCounts);
      }
      // --- 좋아요 상태 및 카운트 초기화 끝 ---

    } catch (error) {
      console.error("구별 사진 조회 실패:", error);
      // alert("네트워크 오류"); // 이미 axios 인터셉터 등에서 처리될 수 있음
    } finally {
      // setLoading(false)
    }
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setSelectedArea("");
    setIsTopBarVisible(true);
    setModalSearchQuery(""); // 검색어 초기화
    setFilteredPosts(null); // 필터링 결과 초기화
  };

  const handleImageUpload = (e) => {
    const files = Array.from(e.target.files); // 새로 선택된 File 객체들
    const newLocalImageUrls = files.map(file => URL.createObjectURL(file)); // 새 파일들의 로컬 URL
    console.log("handleImageUpload: 새로 선택된 파일들:", files, "로컬 URL들:", newLocalImageUrls, "isEditMode:", isEditMode);

    if (isEditMode) {
      // 수정 모드에서는 새로 선택한 이미지들로만 상태를 교체합니다.
      setImage(files); 
      setUploadedImageUrls(newLocalImageUrls);
      console.log("setImage (수정 모드):", files);
      console.log("setUploadedImageUrls (수정 모드):", newLocalImageUrls);
    } else {
      // 새 글 작성 시
      setImage(prev => {
        const newState = [...prev, ...files];
        console.log("setImage (새 글 - 이전 상태):", prev, "새 상태:", newState);
        return newState;
      });
      setUploadedImageUrls(prev => {
        const newState = [...prev, ...newLocalImageUrls];
        console.log("setUploadedImageUrls (새 글 - 이전 상태):", prev, "새 상태:", newState);
        return newState;
      });
    }
  };

  const handleTextChange = (e) => {
    setText(e.target.value);
  };

  const handleImageUploadSubmit = async () => {
    console.log("handleImageUploadSubmit 시작, image 상태:", image, "isEditMode:", isEditMode);
    if (!image || image.length === 0) {
      alert("업로드할 이미지를 선택해주세요.");
      console.log("이미지 없음, 업로드 중단 (handleImageUploadSubmit)");
      return null; // URL 반환을 위해 null 반환
    }

    const formData = new FormData();
    image.forEach((file) => {
      formData.append("files", file);
    });

    try {
      const response = await axios.post("http://localhost:8080/histories/images/upload", formData, {
        headers: {
          // getAuthHeaders에서 Content-Type을 제외하고 가져오거나, 직접 Authorization만 설정
          'Authorization': `Bearer ${sessionStorage.getItem('accessToken')}`,
          // 'Content-Type': 'multipart/form-data' // FormData 사용 시 axios가 자동으로 설정
        }
      });

      if (response.data && response.data.imageUrls) {
        setUploadedImageUrls(response.data.imageUrls); // 상태도 업데이트 (선택적, UI 흐름에 따라)
        // setStep(2); // 이 부분은 새 글 작성 시의 UI 흐름
        // setIsHashtagModalOpen(true); // 이 부분은 새 글 작성 시의 UI 흐름
        // fetchRecommendedTags(); // 이 부분은 새 글 작성 시의 UI 흐름
        return response.data.imageUrls; // 업로드된 S3 URL들을 반환
      } else {
        console.error("S3 이미지 업로드 응답 형식이 잘못되었습니다.", response.data);
        alert("이미지 업로드에 실패했습니다. (서버 응답 오류)");
        return null;
      }
    } catch (err) {
      console.error("이미지 업로드 요청 중 네트워크 오류:", err);
      alert("네트워크 오류가 발생했습니다. 다시 시도해주세요.");
      return null;
    }
  };

  const handlePostSubmit = async () => {
    let finalImageUrls = uploadedImageUrls; // 수정 모드일 경우, 기본적으로 기존 이미지 URL들

    // 수정 모드이고, 새로 선택된 로컬 이미지 파일(image 상태)이 있는 경우 S3에 업로드
    if (isEditMode && image && image.length > 0) {
      console.log("수정 모드: 새로 선택된 이미지를 S3에 업로드합니다.", image);
      const newS3Urls = await handleImageUploadSubmit(); // S3 업로드 함수 호출
      if (newS3Urls && newS3Urls.length > 0) { // newS3Urls가 null이 아니고, 비어있지 않은지 확인
        finalImageUrls = newS3Urls; // 새로 업로드된 S3 URL들로 교체
        // setImage([]); // 업로드 성공했으니 File 객체들은 비워도 됨 (선택적, resetForm에서 처리될 수 있음)
      } else if (newS3Urls === null) { // 명시적으로 업로드 실패 (handleImageUploadSubmit에서 alert 이미 발생)
        alert("이미지 업로드에 실패하여 게시글 수정을 중단합니다.");
        return; // 업로드 실패 시 중단
      }
      // newS3Urls가 빈 배열([])로 오는 경우는 S3 업로드는 성공했으나 URL이 없는 경우이므로,
      // 이 경우 finalImageUrls는 빈 배열이 되고, 이미지 없이 수정 요청을 보낼 수 있음 (백엔드 정책에 따라 다름)
      // 만약 이미지가 필수라면 아래쪽 유효성 검사에서 걸릴 것임
    }

    // 이미지 유효성 검사: finalImageUrls가 비어있는 경우 (백엔드가 이미지를 필수로 요구한다면)
    // 현재 로직은 이미지가 없어도 텍스트만으로 게시글을 생성/수정할 수 있게 되어 있음
    // if (finalImageUrls.length === 0) {
    //   alert("이미지를 업로드해주세요.");
    //   return;
    // }

    if (!text.trim() && finalImageUrls.length === 0) { // 내용도 없고 이미지도 없으면 막기
        alert("게시글 내용이나 이미지를 입력해주세요.");
        return;
    }
    
    if (!text.trim() && isEditMode && finalImageUrls.length === 0) {
      // 수정 모드에서 내용도 없고, 이미지도 모두 삭제하려고 하는 경우 (사용자가 의도적으로 이미지를 비우는 경우)
      // 이 경우 백엔드 API가 imageUrls: [] 를 어떻게 처리하는지에 따라 달라짐
      // 만약 "기존 이미지 삭제"를 의미한다면 이대로 진행 가능
      // 여기서는 일단 이미지를 비우고 내용만 있는 것도 허용하는 현재 로직을 유지
      console.log("수정 모드: 내용이 없고, 이미지도 모두 제거하려고 합니다.");
    } else if (!text.trim() && !isEditMode){ // 새 글 작성인데 내용이 없는 경우 (이미지는 있을 수 있음)
      alert("게시글 내용을 입력해주세요.");
      return;
    }

    const allTagsToSend = [...new Set([...selectedTags, selectedArea])];
    const postData = {
      content: text,
      imageUrls: finalImageUrls, // 최종 이미지 URL 사용
      hashtags: allTagsToSend,
    };

    console.log("[handlePostSubmit] 데이터 전송 시도:", isEditMode ? "수정 모드" : "새 글 모드");
    console.log("[handlePostSubmit] 전송될 postData:", JSON.stringify(postData, null, 2));

    try {
      const endpoint = isEditMode
          ? `http://localhost:8080/histories/newpost/${editingPostId}`
          : "http://localhost:8080/histories/posts";

      const method = isEditMode ? "PUT" : "POST";

      const response = await fetch(endpoint, {
        method: method,
        headers: getAuthHeaders(), 
        body: JSON.stringify(postData),
      });

      if (response.ok) {
        const updatedPostData = await response.json(); 
        console.log("[handlePostSubmit] 서버로부터 받은 updatedPostData:", JSON.stringify(updatedPostData, null, 2));
        
        // 서버로부터 받은 데이터에 images 필드가 있고, 그것이 배열인지 확인
        // 백엔드 PostResponse는 images 필드를 항상 포함하므로, updatedPostData.images를 신뢰할 수 있음
        const responseImages = updatedPostData.images || [];

        alert(isEditMode ? "수정 완료" : "작성 완료");
        
        if (isEditMode && selectedArea && editingPostId) {
          setAreaPosts(prevAreaPosts => {
            const updatedAreaSpecificPosts = (prevAreaPosts[selectedArea] || []).map(post =>
              post.id === editingPostId ? { ...post, ...updatedPostData, images: responseImages } : post
            );
            return { ...prevAreaPosts, [selectedArea]: updatedAreaSpecificPosts };
          });

          if (selectedPost && selectedPost.id === editingPostId) {
            // selectedPost 업데이트 시에도 images 필드를 명시적으로 업데이트
            setSelectedPost(prevSelected => ({ ...prevSelected, ...updatedPostData, images: responseImages }));
          }
        } else if (!isEditMode && selectedArea) {
          setAreaPosts(prevAreaPosts => {
            const currentPosts = prevAreaPosts[selectedArea] || [];
            // 새 글 데이터에도 images 필드가 올바르게 포함되어 있다고 가정
            return { ...prevAreaPosts, [selectedArea]: [{ ...updatedPostData, images: responseImages }, ...currentPosts]}; 
          });
        }
        
        resetForm();
        setIsWriteModalOpen(false);

      } else {
        const errorText = await response.text();
        console.error("서버 오류:", errorText);
        alert("서버 오류 발생");
      }
    } catch (e) {
      console.error("네트워크 오류:", e);
      alert("네트워크 오류");
    }
  };

  const resetForm = () => {
    setText("");
    setImage([]);
    setUploadedImageUrls([]);
    setIsWriteModalOpen(false);
    setStep(1);
    setEditingPostId(null);
    setIsEditMode(false);
    setSelectedTags([]);
    setIsHashtagModalOpen(false);

    // 이미지 URL 해제
    image.forEach(file => {
      if (typeof file === 'string' && file.startsWith('blob:')) {
        URL.revokeObjectURL(file);
      }
    });
  };

  const handleImageClick = async (post) => {
    try {
      // setLoading(true) // 필요하다면 로딩 상태 추가
      const response = await fetch(`http://localhost:8080/histories/filter/${post.id}`, {
        method: "GET",
        // headers: getAuthHeaders(), // 상세 조회는 인증이 필요 없을 수도 있음. 필요시 주석 해제
      });

      if (response.ok) {
        const data = await response.json(); // data는 PostResponse 형태일 것으로 기대
        console.log("게시글 상세 데이터 (handleImageClick):", data);

        setSelectedPost(data);
        setActiveCommentPost(null); // 댓글창은 닫힌 상태로 시작
        setCurrentImageIndex(0);

        // --- 좋아요 상태 및 카운트 초기화 ---
        if (typeof data.isLiked === 'boolean') { // 백엔드에서 isLiked 필드를 준다고 가정
          setLikedPosts((prevLikedPosts) => {
            const updated = new Set(prevLikedPosts);
            if (data.isLiked) {
              updated.add(data.id);
            } else {
              updated.delete(data.id);
            }
            return updated;
          });
        }

        if (typeof data.likeCount === 'number') { // 백엔드에서 likeCount 필드를 준다고 가정
          setLikeCounts(prevLikeCounts => ({
            ...prevLikeCounts,
            [data.id]: data.likeCount
          }));
        }
        // --- 좋아요 상태 및 카운트 초기화 끝 ---

      } else {
        alert("게시글 조회 실패");
        console.error("게시글 조회 실패:", await response.text());
      }
    } catch (err) {
      console.error("게시글 조회 오류:", err);
      alert("네트워크 오류");
    } finally {
      // setLoading(false)
    }
  };

  const handlePostDelete = async () => {
    if (!selectedPost) return;

    if (window.confirm('게시글을 삭제하시겠습니까?')) {
      try {
        const response = await fetch(`http://localhost:8080/histories/delete/${selectedPost.id}`, {
          method: "DELETE",
          headers: getAuthHeaders()
        });

        if (response.ok) {
          alert("게시글이 성공적으로 삭제되었습니다!");
          setAreaPosts((prev) => {
            const updated = { ...prev };
            updated[selectedArea] = updated[selectedArea].filter((p) => p.id !== selectedPost.id);
            return updated;
          });
          setSelectedPost(null);
        } else {
          const errorText = await response.text();
          console.error("삭제 실패:", errorText);
          alert("서버 오류로 삭제에 실패했습니다.");
        }
      } catch (err) {
        console.error("네트워크 오류:", err);
        alert("네트워크 오류 발생");
      }
    }
  };

  const handleLikePost = async (postId) => {
    const currentIsLiked = likedPosts.has(postId); // API 호출 전 프론트엔드 기준 좋아요 상태

    try {
      const endpoint = currentIsLiked
          ? "http://localhost:8080/histories/unlike"
          : "http://localhost:8080/histories/like";

      const response = await fetch(endpoint, {
        method: "POST",
        headers: getAuthHeaders(),
        body: JSON.stringify({ postId: postId }),
      });

      if (response.ok) {
        let serverSaysLiked = currentIsLiked; // 기본값은 현재 상태 유지 시도
        let newLikeCount = likeCounts[postId] || 0;

        if (currentIsLiked) { // 좋아요 취소 요청이었음
          // unlikePost는 성공 시 텍스트 메시지를 반환함 (JSON 아님)
          // 특별히 파싱할 데이터는 없지만, 성공했다는 것 자체가 중요
          serverSaysLiked = false;
          newLikeCount = Math.max(0, (likeCounts[postId] || 1) - 1);
        } else { // 좋아요 요청이었음
          // likePost는 성공 시 boolean (true)을 반환함
          try {
            const likedResult = await response.json(); // boolean 값 파싱 시도
            if (typeof likedResult === 'boolean') {
              serverSaysLiked = likedResult; // 서버가 알려준 좋아요 상태
              if (serverSaysLiked) { // 실제로 좋아요가 되었다면
                   newLikeCount = (likeCounts[postId] || 0) + 1;
              } else {
                  // 서버가 true를 반환하지 않은 경우 (예: 이미 좋아요 되어 있었으나 백엔드가 false를 반환한 경우 - 현재 백엔드 로직상 이 경우는 없음)
                  // 이 경우는 백엔드 로직과 상이하므로, 일단 현재 카운트 유지하거나 +1 하는 것이 맞을 수 있음.
                  // 현재 백엔드는 성공 시 항상 true이므로 이 else는 잘 안 탐.
              }
            }
          } catch (jsonParseError) {
            console.error("좋아요 응답 파싱 오류 (likePost):", jsonParseError);
            // 파싱 실패 시, 일단 UI는 낙관적 업데이트로 currentIsLiked의 반대로 간다고 가정
            // 하지만 서버 상태와 불일치 가능성 있음.
            // 이럴 경우엔 차라리 아무것도 안하거나, 사용자에게 오류 알림이 나을 수 있음.
            // 여기서는 일단 요청이 성공(200 OK)했으니, 프론트엔드에서라도 상태를 반전시킨다.
            serverSaysLiked = !currentIsLiked; 
            if(serverSaysLiked) newLikeCount = (likeCounts[postId] || 0) + 1;
            else newLikeCount = Math.max(0, (likeCounts[postId] || 1) - 1);
          }
        }

        // 최종적으로 결정된 서버 상태 기준으로 프론트엔드 상태 업데이트
        setLikedPosts(prev => {
          const updated = new Set(prev);
          if (serverSaysLiked) {
            updated.add(postId);
          } else {
            updated.delete(postId);
          }
          return updated;
        });

        setLikeCounts(prev => ({
          ...prev,
          [postId]: newLikeCount
        }));

        if (selectedPost && selectedPost.id === postId) {
          setSelectedPost(prevSelected => ({
            ...prevSelected,
            liked: serverSaysLiked,
            // likeCount: newLikeCount // selectedPost에 likeCount가 있다면 이것도 업데이트
          }));
        }

      } else { // response.ok 가 false인 경우 (API 실패)
        let errorMessage = currentIsLiked ? "좋아요 취소 실패" : "좋아요 실패";
        try {
          const errorText = await response.text();
          if (errorText) errorMessage += `: ${errorText}`;
        } catch (textParseError) {
          console.error("에러 메시지 파싱 실패:", textParseError); 
        }
        alert(errorMessage);
        // 실패 시에는 프론트엔드 상태를 롤백하거나 변경하지 않아야 함
      }
    } catch (networkError) { 
      console.error("좋아요 요청 중 네트워크 오류:", networkError);
      alert("네트워크 오류");
    }
  };

  const fetchLikedUsers = async (postId) => {
    // 임시 확인용
    alert(`게시글 ${postId} 좋아요한 사용자 보기 (임시)`);
  };

  const handleCommentChange = (e) => {
    setCommentText(e.target.value);
  };

  const handleCommentSubmit = async (postId) => {
    if (!commentText.trim()) return;

    try {
      let response;
      const requestBody = { content: commentText };

      if (editingCommentId) {
        // 댓글 수정
        response = await fetch(`http://localhost:8080/histories/newcomment/${editingCommentId}`, {
          method: "PUT",
          headers: getAuthHeaders(),
          body: JSON.stringify(requestBody)
        });
      } else if (replyTarget) {
        // 대댓글 작성
        console.log("대댓글 작성 요청:", {
          postId,
          parentCommentId: replyTarget.commentId,
          content: commentText
        });
        response = await fetch(`http://localhost:8080/histories/${postId}/reply/${replyTarget.commentId}`, {
          method: "POST",
          headers: getAuthHeaders(),
          body: JSON.stringify(requestBody)
        });
      } else {
        // 새 댓글 작성
        console.log("새 댓글 작성 요청:", { postId, content: commentText });
        response = await fetch(`http://localhost:8080/histories/comments/${postId}`, {
          method: "POST",
          headers: getAuthHeaders(),
          body: JSON.stringify(requestBody)
        });
      }

      const responseText = await response.text();
      console.log("서버 응답 원본:", responseText);

      if (response.ok) {
        try {
          const newcomment = JSON.parse(responseText);
          console.log("파싱된 새 댓글 데이터:", newcomment);
          
          setComments(prev => {
            const postComments = prev[postId] || [];
            console.log("현재 댓글 목록:", postComments);

            // 댓글과 대댓글을 모두 탐색하며 수정하는 재귀 함수
            const updateNestedComments = (commentsList, targetCommentId, updatedCommentData) => {
              return commentsList.map(comment => {
                // 1. 현재 댓글이 수정 대상인 경우
                if (comment.commentId === targetCommentId) {
                  return { ...comment, ...updatedCommentData }; // 새 데이터로 업데이트
                }
                // 2. 현재 댓글에 대댓글(replies)이 있고, 그 안에 수정 대상이 있을 수 있는 경우
                if (comment.replies && comment.replies.length > 0) {
                  return {
                    ...comment,
                    replies: updateNestedComments(comment.replies, targetCommentId, updatedCommentData) // 재귀 호출
                  };
                }
                // 3. 수정 대상이 아닌 경우
                return comment;
              });
            };

            let updatedComments;
            if (editingCommentId) { // 수정 모드일 때
              updatedComments = updateNestedComments(postComments, editingCommentId, newcomment);
            } else if (replyTarget) { // 대댓글 작성 모드일 때
              updatedComments = postComments.map(c => {
                if (c.commentId === replyTarget.commentId) {
                  return {
                    ...c,
                    replies: [...(c.replies || []), newcomment]
                  };
                }
                return c;
              });
            } else { // 새 댓글 작성 모드일 때
              updatedComments = [...postComments, newcomment];
            }
            
            console.log("업데이트될 댓글 목록:", updatedComments);
            return { ...prev, [postId]: updatedComments };
          });

          setCommentText("");
          setEditingCommentId(null);
          setReplyTarget(null);
        } catch (parseError) {
          console.error("댓글 데이터 파싱 실패:", parseError);
          alert("서버 응답을 처리할 수 없습니다.");
        }
      } else {
        console.error("댓글 처리 실패 - 상태 코드:", response.status);
        alert("댓글 처리에 실패했습니다.");
      }
    } catch (error) {
      console.error("댓글 처리 중 오류:", error);
      alert("네트워크 오류가 발생했습니다.");
    }
  };

  const handleDeleteComment = async (postId, commentId, isReply = false, parentCommentId = null) => {
    console.log("삭제 시도:", { postId, commentId, isReply, parentCommentId });
    if (window.confirm('댓글을 삭제하시겠습니까?')) {
      try {
        const response = await fetch(`http://localhost:8080/histories/${commentId}`, {
          method: "DELETE",
          headers: getAuthHeaders()
        });

        console.log("삭제 응답:", response.status);
        const responseText = await response.text();
        console.log("삭제 응답 내용:", responseText);

        if (response.ok) {
          setComments(prev => {
            const updated = { ...prev };
            console.log("삭제 전 댓글 상태:", updated[postId]);
            
            if (isReply && parentCommentId !== null) {
              updated[postId] = updated[postId].map(comment => {
                if (comment.commentId === parentCommentId) {
                  return {
                    ...comment,
                    replies: (comment.replies || []).filter(r => r.commentId !== commentId)
                  };
                }
                return comment;
              });
            } else {
              updated[postId] = updated[postId].filter(c => c.commentId !== commentId);
            }
            
            console.log("삭제 후 댓글 상태:", updated[postId]);
            return updated;
          });
        } else {
          alert("댓글 삭제 실패");
        }
      } catch (err) {
        console.error("삭제 중 오류:", err);
        alert("네트워크 오류");
      }
    }
  };

  const startEditComment = (comment, isReply = false, parentCommentId = null) => {
    setCommentText(comment.content);
    setEditingCommentId(comment.commentId);

    if (isReply && parentCommentId !== null) {
      setReplyTarget({ postId: activeCommentPost.id, commentId: parentCommentId });
    } else {
      setReplyTarget(null);
    }
  };

  const toggleComments = async (post) => {
    if (activeCommentPost && activeCommentPost.id === post.id) {
      setActiveCommentPost(null); // 같은 게시글의 댓글창이면 닫기
      setReplyTarget(null); // 대댓글 타겟 초기화
      setCommentText('');   // 댓글 입력창 초기화
    } else {
      setActiveCommentPost(post); // 다른 게시글이거나 닫혀있으면 열기
      setReplyTarget(null);
      setCommentText('');

      // 댓글을 새로 불러와야 하는 경우:
      // 1. 현재 comments 상태에 해당 postId에 대한 댓글이 없거나
      // 2. 또는 이미 있지만 (예: 이전에 일부만 로드된 경우 - 지금은 전체 로드로 가정하므로 없다고 간주해도 무방)
      //    새로고침을 원한다면 추가 로직 필요. 여기서는 '없을 때만 로드'로 단순화.
      if (!comments[post.id] || comments[post.id].length === 0) {
        setIsLoadingComments(true); // 로딩 시작
        try {
          console.log(`댓글 로딩 시도: postId = ${post.id}`);
          const response = await fetch(`http://localhost:8080/histories/${post.id}/all`, {
            method: "GET",
            headers: getAuthHeaders(), // 인증 헤더 포함
          });

          if (response.ok) {
            const fetchedComments = await response.json();
            console.log(`불러온 댓글 (postId: ${post.id}):`, fetchedComments);
            setComments(prevComments => ({
              ...prevComments,
              [post.id]: fetchedComments // 해당 postId에 대한 댓글 목록 업데이트
            }));
          } else {
            console.error(`댓글 로딩 실패 (postId: ${post.id}):`, response.status, await response.text());
            alert("댓글을 불러오는데 실패했습니다.");
            // 댓글 로딩 실패 시 activeCommentPost를 다시 null로 할지, 아니면 빈 댓글창을 보여줄지 결정
            // 여기서는 일단 댓글창은 열어두고, 에러 메시지만 표시
          }
        } catch (error) {
          console.error(`댓글 로딩 중 네트워크 오류 (postId: ${post.id}):`, error);
          alert("댓글 로딩 중 네트워크 오류가 발생했습니다.");
        } finally {
          setIsLoadingComments(false); // 로딩 종료
        }
      } else {
        console.log(`이미 로드된 댓글 사용 (postId: ${post.id})`);
      }
    }
  };

  const fetchRecommendedTags = async () => {
    try {
      const response = await fetch("http://localhost:8080/histories/recommendation", {
        method: "GET",
      });

      if (response.ok) {
        const tags = await response.json();
        setRecommendedTags(tags);
      } else {
        alert("해시태그 추천 실패");
      }
    } catch (error) {
      console.error("추천 요청 실패:", error);
      alert("네트워크 오류");
    }
  };

  const toggleTagSelection = (tag) => {
    setSelectedTags((prev) => {
      if (prev.includes(tag)) {
        return prev.filter((t) => t !== tag);
      } else if (prev.length < 3) {
        return [...prev, tag];
      } else {
        alert("최대 3개까지 선택할 수 있습니다.");
        return prev;
      }
    });
  };

  const toggleReply = (postId, commentId) => {
    console.log("💬 대댓글 타겟 설정:", { postId, commentId });
    if (replyTarget && replyTarget.commentId === commentId) {
      setReplyTarget(null); // 같은 댓글의 답글 버튼을 누르면 취소
    } else {
      setReplyTarget({ postId, commentId }); // 다른 댓글의 답글 버튼을 누르면 설정
      setCommentText(''); // 입력창 초기화
    }
  };

  const nextImage = () => {
    if (selectedPost) {
      setCurrentImageIndex((prev) =>
          prev === selectedPost.images.length - 1 ? 0 : prev + 1
      );
    }
  };

  const prevImage = () => {
    if (selectedPost) {
      setCurrentImageIndex((prev) =>
          prev === 0 ? selectedPost.images.length - 1 : prev - 1
      );
    }
  };

  const goToImage = (index) => {
    setCurrentImageIndex(index);
  };

  // 벚꽃 애니메이션 메모이제이션
  const cherryBlossoms = useMemo(() => {
    if (isNightTheme) return null;

    return [...Array(10)].map((_, i) => (
        <img
            key={i}
            src="/images/cherry_blossom.png"
            className="falling-blossom"
            style={{
              left: `${Math.random() * 100}vw`,
              animationDelay: `${Math.random() * 5}s`,
              animationDuration: `${5 + Math.random() * 5}s`,
            }}
            alt="cherry"
        />
    ));
  }, [isNightTheme]);

  // 달과 파티클 애니메이션 메모이제이션
  const moonAndParticles = useMemo(() => {
    if (!isNightTheme) return null;

    return (
        <>
          <img src="/images/moon.png" alt="moon" className="moon-image" />
          {[...Array(15)].map((_, i) => (
              <div
                  key={i}
                  className="moon-particle"
                  style={{
                    left: `${Math.random() * 100}vw`,
                    animationDelay: `${Math.random() * 4}s`,
                    animationDuration: `${5 + Math.random() * 5}s`,
                  }}
              />
          ))}
        </>
    );
  }, [isNightTheme]);

  // 해 애니메이션 메모이제이션 제거
  const sunAnimation = useMemo(() => {
    if (isNightTheme) return null;

    return null; // 해 애니메이션 비활성화
  }, [isNightTheme]);

  // 게시글 수정 시작 시 해시태그도 불러오기
  const startEditPost = (post) => {
    console.log("게시글 수정 시작 데이터 (startEditPost):", post);
    setStep(2);
    setText(post.content || post.text || "");
    setUploadedImageUrls(post.images || []);
    setEditingPostId(post.id);
    setIsEditMode(true);
    setSelectedPost(null);
    setIsWriteModalOpen(true);
    setCurrentImageIndex(0);
    setSelectedTags(post.postImageHashtags ? post.postImageHashtags.map(tag => tag.hashtag.name) : []);
    setImage([]);
  };

  const handleHashtagModalClose = () => {
    setIsHashtagModalOpen(false);
    setStep(2);
  };

  const PostHashtags = ({ tags }) => {
    if (!tags || tags.length === 0) return null;

    return (
        <div className="post-hashtags">
          {tags.map((tag, index) => (
              <span key={index} className="post-hashtag">{tag}</span>
          ))}
        </div>
    );
  };

  // 해시태그 필터링 로직
  const getFilteredHashtags = () => {
    if (!searchQuery.startsWith('#')) return [];
    const searchTerm = searchQuery.slice(1).toLowerCase();
    return Object.keys(hashtagToGusMapping).filter(tag =>
        tag.toLowerCase().includes(searchTerm)
    );
  };

  // 인기 해시태그 데이터 가져오기
  const fetchPopularHashtags = async () => {
    try {
      const response = await fetch('http://localhost:8080/photos', {
        method: 'GET',
        headers: getAuthHeaders()
      });

      if (response.ok) {
        const photosData = await response.json();

        // 해시태그 사용 횟수 계산
        const hashtagCounts = {};
        photosData.content.forEach(photo => {
          photo.postImageHashtags.forEach(tag => {
            const hashtagName = tag.hashtag.name;
            hashtagCounts[hashtagName] = (hashtagCounts[hashtagName] || 0) + 1;
          });
        });

        // 상위 3개 추출
        const topHashtags = Object.entries(hashtagCounts)
            .map(([name, count]) => ({
              id: name,
              name: name,
              count: count
            }))
            .sort((a, b) => b.count - a.count)
            .slice(0, 3);

        return topHashtags;
      } else {
        console.error('해시태그 가져오기 실패');
        return [];
      }
    } catch (error) {
      console.error('해시태그 요청 중 오류:', error);
      return [];
    }
  };

  return (
      <div className={`main-page ${isNightTheme ? "night" : "day"}`}>
        {userData && (
          <MPProfile isMainPage={true} isMyPage={true} userData={userData} setIsLogined={setIsLogined} />
        )}

        {/* 테마 토글 버튼 */}
        <div className="theme-toggle" onClick={toggleTheme}>
          <span className="toggle-button">{isNightTheme ? "☀️" : "🌙"}</span>
        </div>

        {/* 벚꽃 애니메이션 */}
        {!isNightTheme && cherryBlossoms}

        {/* 달과 파티클 애니메이션 */}
        {isNightTheme && moonAndParticles}

        {/* 해 애니메이션 추가 */}
        {!isNightTheme && sunAnimation}

        {isTopBarVisible && (
            <div className="top-bar">
              <div className="search-bar-container">
                <FiSearch className="search-icon" />
                <input
                    type="text"
                    placeholder="Search district or hashtags..."
                    value={searchQuery}
                    onChange={handleSearchChange}
                    onKeyPress={handleSearchKeyPress}
                />
                <div className={`hashtag-suggestions ${showHashtags ? 'show' : ''}`}>
                  {getFilteredHashtags().map((tag, index) => (
                      <div
                          key={index}
                          className="hashtag"
                          onClick={() => handleHashtagClick(tag)}
                      >
                        {tag.replace('#', '')}
                      </div>
                  ))}
                </div>
              </div>
              <div className="slogan">
                <div className="slogan-line">Capture the Moment</div>
                <div className="slogan-line">Keep it Forever</div>
              </div>
            </div>
        )}

        <div className="seoul-map-container">
          <SeoulMap onAreaClick={openModal} highlightedGus={highlightedGus} isNightTheme={isNightTheme} />
        </div>

        <ReactModal
            isOpen={isModalOpen}
            onRequestClose={closeModal}
            className="modal"
            overlayClassName="overlay"
        >
          <div className="modal-header">
            <h2 className="modal-title">{selectedArea}</h2>
            <div className="modal-search-container">
              <input
                  type="text"
                  placeholder="#해시태그로 검색"
                  value={modalSearchQuery}
                  onChange={handleModalSearch}
                  onKeyPress={handleModalSearchKeyPress}
                  className="modal-search-input"
              />
            </div>
            <button className="modal-close" onClick={closeModal}>
              <FiX />
            </button>
          </div>

          <div className="modal-images-container">
            <div className="row-top">
              {(filteredPosts || areaPosts[selectedArea] || []).flatMap((post, idx) =>
                  post.images.slice(0, 5).map((img, i) => (
                      <div
                          key={`${idx}-top-${i}`}
                          className="modal-filled"
                          onClick={() => handleImageClick(post)}
                      >
                        <img src={img} alt="uploaded" />
                        {post.postImageHashtags && post.postImageHashtags.length > 0 && (
                            <div className="image-hashtags">
                              {post.postImageHashtags.map((tag, tagIdx) => (
                                  <span key={tagIdx} className="image-hashtag">#{tag.hashtag.name}</span>
                              ))}
                            </div>
                        )}
                      </div>
                  ))
              )}
            </div>
            <div className="row-bottom">
              {(filteredPosts || areaPosts[selectedArea] || []).flatMap((post, idx) =>
                  post.images.slice(5).map((img, i) => (
                      <div
                          key={`${idx}-bottom-${i}`}
                          className="modal-filled"
                          onClick={() => handleImageClick(post)}
                      >
                        <img src={img} alt="uploaded" />
                        {post.postImageHashtags && post.postImageHashtags.length > 0 && (
                            <div className="image-hashtags">
                              {post.postImageHashtags.map((tag, tagIdx) => (
                                  <span key={tagIdx} className="image-hashtag">#{tag.hashtag.name}</span>
                              ))}
                            </div>
                        )}
                      </div>
                  ))
              )}
            </div>
          </div>

          <button className="write-button" onClick={openWriteModal}>
            <FiEdit2 /> 글쓰기
          </button>
        </ReactModal>

        <ReactModal
            isOpen={isWriteModalOpen}
            onRequestClose={closeWriteModal}
            className="modal"
            overlayClassName="overlay"
        >
          <div className="modal-header">
            <h2 className="modal-title">{isEditMode ? "게시글 수정" : "새 게시글 작성"}</h2>
            <button className="modal-close" onClick={closeWriteModal}>
              <FiX />
            </button>
          </div>

          <div className="write-form">
            {isEditMode ? (
              // 수정 모드 UI
              <div className="edit-section"> 
                {/* 현재 이미지 미리보기 섹션 */}
                <div className="current-images-preview" style={{ marginBottom: '20px' }}>
                  <h4>현재 이미지</h4>
                  {uploadedImageUrls && uploadedImageUrls.length > 0 ? (
                    <div className="preview-images">
                      {uploadedImageUrls.map((url, index) => (
                        <div key={index} className="preview-image-container">
                          <img src={url} alt={`Current ${index + 1}`} style={{ maxWidth: '100px', maxHeight: '100px', marginRight: '10px' }} />
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p>업로드된 이미지가 없습니다.</p>
                  )}
                </div>

                {/* 이미지 변경을 위한 파일 입력 섹션 */}
                <div className="upload-section-minimal" style={{ marginBottom: '20px' }}>
                  <label htmlFor="image-upload-edit" className="upload-label-button" 
                         style={{ padding: '10px 15px', backgroundColor: '#f0f0f0', border: '1px solid #ccc', borderRadius: '4px', cursor: 'pointer' }}>
                    <FiEdit2 size={18} style={{ marginRight: '5px' }} /> 이미지 변경
                  </label>
                  <input
                    type="file"
                    multiple
                    accept="image/*"
                    onChange={handleImageUpload} // handleImageUpload는 isEditMode를 인지함
                    id="image-upload-edit"
                    style={{ display: 'none' }} // 실제 input은 숨김
                  />
                </div>

                {/* 새로 선택한 이미지 미리보기 섹션 */}
                {image && image.length > 0 && (
                  <div className="new-images-preview" style={{ marginBottom: '20px' }}>
                    <h4>새로 선택한 이미지 (기존 이미지를 대체합니다)</h4>
                    <div className="preview-images">
                      {image.map((file, index) => (
                        <div key={`new-${index}`} className="preview-image-container">
                          <img src={URL.createObjectURL(file)} alt={`New preview ${index + 1}`} style={{ maxWidth: '100px', maxHeight: '100px', marginRight: '10px' }} />
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* 텍스트 입력 영역 */}
                <textarea
                  placeholder="게시글 내용을 입력하세요..."
                  value={text}
                  onChange={handleTextChange}
                  className="post-textarea"
                  style={{ width: '100%', minHeight: '100px', marginBottom: '10px' }}
                />

                {/* 해시태그 선택 영역 (기존과 유사) */}
                <div className="selected-tags" style={{ marginBottom: '20px' }}>
                  {selectedTags.map((tag, index) => (
                    <span key={index} className="tag" 
                          style={{ background: '#e0e0e0', padding: '5px 10px', borderRadius: '15px', marginRight: '5px', display: 'inline-flex', alignItems: 'center' }}>
                      {tag}
                      <button onClick={() => setSelectedTags(prev => prev.filter(t => t !== tag))} 
                              style={{ background: 'none', border: 'none', color: '#757575', marginLeft: '5px', cursor: 'pointer' }}>
                        <FiX />
                      </button>
                    </span>
                  ))}
                </div>
                
                {/* 버튼 그룹 */}
                <div className="button-group">
                  <button className="btn" onClick={handlePostSubmit}>
                    수정하기
                  </button>
                </div>
              </div>
            ) : step === 1 ? (
              // 새 글 작성 - 이미지 업로드 단계 (기존 UI)
              <div className="upload-section">
                <div className="upload-zone">
                  <input
                      type="file"
                      multiple
                      accept="image/*"
                      onChange={handleImageUpload}
                      id="image-upload"
                      className="hidden"
                  />
                  <label htmlFor="image-upload" className="upload-label">
                    <div className="upload-content">
                      <FiEdit2 size={24} />
                      <p>클릭하여 이미지 업로드</p>
                      <span className="upload-hint">또는 이미지를 여기에 드래그하세요</span>
                    </div>
                  </label>
                </div>

                {image.length > 0 && (
                    <div className="preview-images">
                      {image.map((file, index) => (
                          <div key={index} className="preview-image-container">
                            <img
                                src={typeof file === 'string' ? file : URL.createObjectURL(file)}
                                alt={`Preview ${index + 1}`}
                            />
                            <button
                                className="remove-image"
                                onClick={() => {
                                  // 새 글 작성 시 이미지 제거 로직
                                  const newImages = image.filter((_, i) => i !== index);
                                  const newImageUrls = uploadedImageUrls.filter((_, i) => i !== index); // uploadedImageUrls도 동기화
                                  setImage(newImages);
                                  setUploadedImageUrls(newImageUrls);
                                }}
                            >
                              <FiX />
                            </button>
                          </div>
                      ))}
                    </div>
                )}

                {image.length > 0 && (
                    <div className="button-group">
                      <button 
                        className="btn" 
                        onClick={() => {
                          console.log("'다음' 버튼 클릭됨. isEditMode:", isEditMode, "현재 image 상태:", image);
                          if (!isEditMode) {
                            console.log("handleImageUploadSubmit 호출 시도 (새 글 작성 모드)");
                            const uploadAndProceed = async () => {
                              const s3Urls = await handleImageUploadSubmit();
                              if (s3Urls && s3Urls.length > 0) {
                                setStep(2);
                                setIsHashtagModalOpen(true);
                                // fetchRecommendedTags(); 
                              }
                            };
                            uploadAndProceed();
                          } else {
                            console.log("수정 모드이므로 '다음' 버튼 로직 실행 안 함 (버튼은 비활성화 되어야 함)");
                          }
                        }} 
                        disabled={isEditMode} 
                      >
                        다음
                      </button>
                    </div>
                )}
              </div>
            ) : (
              // 새 글 작성 - 텍스트 입력 단계 (기존 UI)
              <div className="text-section">
                <textarea
                    placeholder="게시글 내용을 입력하세요..."
                    value={text}
                    onChange={handleTextChange}
                    className="post-textarea"
                />
                <div className="selected-tags">
                  {selectedTags.map((tag, index) => (
                      <span key={index} className="tag">
                  {tag}
                        <button
                            onClick={() => setSelectedTags(prev => prev.filter(t => t !== tag))}
                        >
                    <FiX />
                  </button>
                </span>
                  ))}
                </div>
                <div className="button-group">
                  <button className="btn btn-secondary" onClick={() => setStep(1)}>이전</button>
                  <button className="btn" onClick={handlePostSubmit}>작성하기</button>
                </div>
              </div>
            )}
          </div>
        </ReactModal>

        {selectedPost && (
            <ReactModal
                isOpen={true}
                onRequestClose={() => {
                  setSelectedPost(null);
                  setActiveCommentPost(null);
                  setReplyTarget(null);
                  setCommentText('');
                  setCurrentImageIndex(0);
                }}
                className="modal"
                overlayClassName="overlay"
            >
              <div className="modal-header">
                <h2 className="modal-title">게시글</h2>
                <button className="modal-close" onClick={() => {
                  setSelectedPost(null);
                  setCurrentImageIndex(0);
                }}>
                  <FiX />
                </button>
              </div>

              <div className={`post-detail ${activeCommentPost ? 'with-comments' : ''}`}>
                <div className="post-content">
                  <div className="post-images">
                    <div style={{
                      display: 'flex',
                      transform: `translateX(-${currentImageIndex * 100}%)`,
                      transition: 'transform 0.3s ease',
                      width: '100%',
                      height: '100%'
                    }}>
                      {selectedPost.images.map((img, idx) => (
                          <img key={idx} src={img} alt={`post-img-${idx}`} />
                      ))}
                    </div>
                    {selectedPost.images.length > 1 && (
                        <>
                          <div className="image-slider-controls">
                            <button className="slider-button" onClick={prevImage}>
                              <FiChevronLeft />
                            </button>
                            <button className="slider-button" onClick={nextImage}>
                              <FiChevronRight />
                            </button>
                          </div>
                          <div className="slider-dots">
                            {selectedPost.images.map((_, idx) => (
                                <div
                                    key={idx}
                                    className={`slider-dot ${idx === currentImageIndex ? 'active' : ''}`}
                                    onClick={() => goToImage(idx)}
                                />
                            ))}
                          </div>
                        </>
                    )}
                  </div>

                  <div className="post-text-container">
                    <p className="post-text">{selectedPost.content || selectedPost.text}</p>
                    <PostHashtags tags={selectedPost.postImageHashtags?.map(tag => tag.hashtag.name)} />
                  </div>

                  <div className="post-actions">
                    <button
                        className={`action-btn ${likedPosts.has(selectedPost.id) ? "liked" : ""}`}
                        onClick={() => handleLikePost(selectedPost.id)}
                    >
                      <FiHeart />
                      <span>{likeCounts[selectedPost.id] || 0}</span>
                    </button>

                    <button
                        className={`action-btn comment-btn ${activeCommentPost ? 'active' : ''}`}
                        onClick={() => toggleComments(selectedPost)}
                    >
                      <FiMessageCircle />
                      <span>댓글</span>
                    </button>

                    <button
                        className="action-btn"
                        onClick={() => fetchLikedUsers(selectedPost.id)}
                    >
                      <FiHeart />
                      <span>좋아요 목록</span>
                    </button>

                    <button
                        className="action-btn edit-btn"
                        onClick={() => startEditPost(selectedPost)}
                    >
                      <FiEdit2 />
                      <span>수정</span>
                    </button>

                    <button
                        className="action-btn delete-btn"
                        onClick={handlePostDelete}
                    >
                      <FiTrash2 />
                      <span>삭제</span>
                    </button>
                  </div>
                </div>

                {activeCommentPost && (
                    <div className="comments-section">
                      <button className="comments-close" onClick={() => {
                        setActiveCommentPost(null);
                        setReplyTarget(null);
                        setCommentText('');
                      }}>
                        <FiX />
                      </button>

                      <div className="comments-list">
                        {isLoadingComments && activeCommentPost && (!comments[activeCommentPost.id] || comments[activeCommentPost.id].length === 0) ? (
                          <p>댓글을 불러오는 중입니다...</p>
                        ) : (comments[activeCommentPost.id] || []).length === 0 ? (
                          <p>아직 댓글이 없습니다.</p>
                        ) : (
                          (comments[activeCommentPost.id] || []).map((comment) => (
                            <div key={comment.commentId} className="comment-item">
                              <div className="comment-content">
                                <strong>{comment.memberName || '사용자'}</strong>
                                <p>{comment.content}</p>

                                <div className="comment-actions">
                                  <button onClick={() => startEditComment(comment)}>
                                    <FiEdit2 />
                                  </button>
                                  <button onClick={() => handleDeleteComment(activeCommentPost.id, comment.commentId)}>
                                    <FiTrash2 />
                                  </button>
                                  <button
                                      className={`reply-button ${replyTarget?.commentId === comment.commentId ? 'active' : ''}`}
                                      onClick={() => toggleReply(activeCommentPost.id, comment.commentId)}
                                  >
                                    <FiCornerDownRight />
                                    <span>답글</span>
                                  </button>
                                </div>
                              </div>

                              {(comment.replies || []).map((reply) => (
                                  <div key={reply.commentId} className="reply-item">
                                    <div className="comment-content">
                                      <strong>{reply.memberName || '답글'}</strong>
                                      <p>{reply.content}</p>

                                      <div className="comment-actions">
                                        <button onClick={() => startEditComment(reply, true, comment.commentId)}>
                                          <FiEdit2 />
                                        </button>
                                        <button onClick={() => handleDeleteComment(activeCommentPost.id, reply.commentId, true, comment.commentId)}>
                                          <FiTrash2 />
                                        </button>
                                      </div>
                                    </div>
                                  </div>
                              ))}
                            </div>
                          ))
                        )}
                      </div>

                      <div className="comment-input">
                        {replyTarget && (
                            <div className="reply-indicator">
                              <FiCornerDownRight />
                              <span>답글 작성 중</span>
                              <button onClick={() => setReplyTarget(null)}>
                                <FiX />
                              </button>
                            </div>
                        )}
                        <textarea
                            value={commentText}
                            onChange={handleCommentChange}
                            placeholder={replyTarget ? "답글을 입력하세요..." : "댓글을 입력하세요..."}
                        />
                        <button
                            className="btn"
                            onClick={() => handleCommentSubmit(activeCommentPost.id)}
                        >
                          <FiSend />
                        </button>
                      </div>
                    </div>
                )}
              </div>
            </ReactModal>
        )}

        {isHashtagModalOpen && (
            <ReactModal
                isOpen={true}
                onRequestClose={handleHashtagModalClose}
                className="modal"
                overlayClassName="overlay"
            >
              <div className="modal-header">
                <h2 className="modal-title">AI 추천 해시태그 (최대 3개)</h2>
                <button className="modal-close" onClick={handleHashtagModalClose}>
                  <FiX />
                </button>
              </div>

              <div className="hashtag-container">
                {recommendedTags.map((tag, i) => (
                    <div
                        key={i}
                        className={`recommended-tag ${selectedTags.includes(tag) ? "selected" : ""}`}
                        onClick={() => toggleTagSelection(tag)}
                    >
                      {tag}
                    </div>
                ))}
              </div>

              <div className="hashtag-modal-footer">
                <button className="btn" onClick={handleHashtagModalClose}>
                  적용하기
                </button>
              </div>
            </ReactModal>
        )}

        {showHashtags && (
            <HashtagBubbles
                hashtags={popularHashtags}
                onHashtagClick={handleHashtagClick}
            />
        )}
      </div>
  );
};

export default MainPage;
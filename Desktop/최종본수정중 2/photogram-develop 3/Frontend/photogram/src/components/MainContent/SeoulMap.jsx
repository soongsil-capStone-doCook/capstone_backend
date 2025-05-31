import { useState, useEffect } from "react";
import "./SeoulMap.css";

const SeoulMap = ({ onAreaClick, highlightedGus, isNightTheme }) => {
  const [hoveredGu, setHoveredGu] = useState(null);
  const [selectedGu, setSelectedGu] = useState(null);
  const [pathsData, setPathsData] = useState([]);

  // highlightedGus가 변경될 때 selectedGu 초기화
  useEffect(() => {
    if (highlightedGus && highlightedGus.length > 0) {
      setSelectedGu(null);
    }
  }, [highlightedGus]);

  // 구 이름 매핑 (SVG의 id와 실제 구 이름 매핑)
  const guNameMapping = {
    'Gangnam-gu': '강남',
    'Seocho-gu': '서초',
    'Songpa-gu': '송파',
    'Gangdong-gu': '강동',
    'Gwangjin-gu': '광진',
    'Jungnang-gu': '중랑',
    'Dongdaemun-gu': '동대문',
    'Seongbuk-gu': '성북',
    'Gangbuk-gu': '강북',
    'Dobong-gu': '도봉',
    'Nowon-gu': '노원',
    'Eunpyeong-gu': '은평',
    'Seodaemun-gu': '서대문',
    'Mapo-gu': '마포',
    'Yongsan-gu': '용산',
    'Jung-gu': '중구',
    'Seongdong-gu': '성동',
    'Yeongdeungpo-gu': '영등포',
    'Dongjak-gu': '동작',
    'Gwanak-gu': '관악',
    'Geumcheon-gu': '금천',
    'Guro-gu': '구로',
    'Gangseo-gu': '강서',
    'Yangcheon-gu': '양천',
    'Jongno-gu': '종로'
  };

  useEffect(() => {
    fetch("/seoul_test.svg")
      .then((res) => res.text())
      .then((svgText) => {
        const parser = new DOMParser();
        const svgDoc = parser.parseFromString(svgText, "image/svg+xml");
        const paths = svgDoc.querySelectorAll("path");

        const extractedPaths = Array.from(paths).map((path) => ({
          id: path.getAttribute("id"),
          d: path.getAttribute("d"),
          name: guNameMapping[path.getAttribute("id")] || path.getAttribute("id"),
          fill: "rgba(255, 255, 255, 0.8)",
        }));

        setPathsData(extractedPaths);
      })
      .catch((err) => console.error("SVG 로드 실패:", err));
  }, []);

  const handleMouseEnter = (gu) => setHoveredGu(gu);
  const handleMouseLeave = () => setHoveredGu(null);
  const handleClick = (gu) => {
    setSelectedGu(gu);
    onAreaClick(guNameMapping[gu] + "구");
  };

  const getPathStyle = (path) => {
    const isHighlighted = highlightedGus && highlightedGus.includes(guNameMapping[path.id]);
    const isHovered = hoveredGu === path.id;
    const isSelected = selectedGu === path.id;

    return {
      fill: isHighlighted 
        ? isNightTheme 
          ? 'rgba(100, 181, 246, 0.4)'
          : 'rgba(255, 192, 203, 0.6)'
        : isSelected 
          ? "rgba(255, 165, 0, 0.6)" 
          : path.fill,
      stroke: isHighlighted 
        ? isNightTheme
          ? "rgba(100, 181, 246, 0.8)"
          : "#ff69b4"
        : isSelected || isHovered 
          ? "#ff9f1a" 
          : "#666",
      strokeWidth: (isHighlighted || isHovered || isSelected) ? "3" : "0.5",
      cursor: "pointer",
      transition: "all 0.3s ease"
    };
  };

  return (
    <div className="seoul-map-container">
      <svg
        viewBox="0 0 1600 1600"
        preserveAspectRatio="xMidYMid meet"
        xmlns="http://www.w3.org/2000/svg"
      >
        {pathsData.map((path) => (
          <path
            key={path.id}
            id={path.id}
            d={path.d}
            style={getPathStyle(path)}
            onClick={() => handleClick(path.id)}
            onMouseEnter={() => handleMouseEnter(path.id)}
            onMouseLeave={handleMouseLeave}
          />
        ))}
      </svg>
    </div>
  );
};

export default SeoulMap;

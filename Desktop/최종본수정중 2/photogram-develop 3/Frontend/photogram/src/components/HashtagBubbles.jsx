import React, { useState, useEffect } from 'react';

const HashtagBubbles = ({ hashtags, onHashtagClick }) => {
  const [showBubbles, setShowBubbles] = useState(false);

  useEffect(() => {
    if (hashtags.length > 0) {
      setShowBubbles(true);
    } else {
      setShowBubbles(false);
    }
  }, [hashtags]);

  return (
    <div className="hashtag-bubbles">
      {hashtags.slice(0, 3).map((hashtag, index) => (
        <div
          key={index}
          className={`hashtag-bubble ${showBubbles ? 'show' : ''}`}
          onClick={() => onHashtagClick(hashtag)}
          style={{ animationDelay: `${index * 0.2}s` }}
        >
          {hashtag}
        </div>
      ))}
    </div>
  );
};

export default HashtagBubbles; 
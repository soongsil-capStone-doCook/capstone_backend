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
          key={hashtag.id}
          className={`hashtag-bubble ${showBubbles ? 'show' : ''}`}
          onClick={() => onHashtagClick(hashtag.name)}
          style={{ animationDelay: `${index * 0.2}s` }}
        >
          #{hashtag.name} · {hashtag.count}
        </div>
      ))}
    </div>
  );
};

export default HashtagBubbles; 
package codeshot.photogram.domain.hashtag.application;

import codeshot.photogram.domain.hashtag.domain.entity.Hashtag;
import codeshot.photogram.domain.hashtag.dto.PostImageWithHashtags;
import codeshot.photogram.domain.post.domain.entity.PostImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

public interface HashtagService {

    List<Hashtag> createHashtagsFromUrl(String imageUrl) throws IOException;
    Page<PostImageWithHashtags> searchPhotosByHashtags(List<String> hashtags, Pageable pageable);
    List<String> extractHashtagsFromImage(String imageUrl);
    Hashtag createOrGet(String hashtagName);

    List<String> searchAllHashtag();
    Page<PostImage> searchUserPhotosByHashtags(List<String> hashtags, Pageable pageable, Long memberId);
}


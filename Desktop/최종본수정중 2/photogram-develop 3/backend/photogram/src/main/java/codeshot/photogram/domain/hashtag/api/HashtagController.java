package codeshot.photogram.domain.hashtag.api;

import codeshot.photogram.domain.hashtag.application.HashtagService;
import codeshot.photogram.domain.hashtag.application.HashtagServiceImpl;
import codeshot.photogram.domain.hashtag.domain.entity.Hashtag;
import codeshot.photogram.domain.hashtag.dto.HashtagRequestDTO;
import codeshot.photogram.domain.hashtag.dto.HashtagResponseDTO;
import codeshot.photogram.domain.hashtag.dto.PostImageWithHashtags;
import codeshot.photogram.domain.post.domain.entity.PostImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/histories")
public class HashtagController {

    private final HashtagService hashtagService;

    public HashtagController(HashtagServiceImpl hashtagService) {
        this.hashtagService = hashtagService;
    }

    // 여러 해시태그로 사진 검색
    @GetMapping("/photos")
    public Page<PostImageWithHashtags> searchPhotosByHashtags(@RequestParam Pageable pageable,
                                                              @RequestBody HashtagRequestDTO request) {
        return hashtagService.searchPhotosByHashtags(request.getHashtags(), pageable);
    }

    @GetMapping("/user/photos")
    public Page<PostImage> searchUserPhotosByHashtags(@RequestParam Pageable pageable,
                                                      @RequestBody HashtagRequestDTO request,
                                                      @AuthenticationPrincipal(expression = "username") String memberIdStr) {
        Long memberId = Long.valueOf(memberIdStr);
        return hashtagService.searchUserPhotosByHashtags(request.getHashtags(), pageable, memberId);
    }

    // 이미지 URL로 해시태그 생성
    @PostMapping("/recommendation")
    public List<HashtagResponseDTO> createHashtagsFromUrl(@RequestBody HashtagRequestDTO request) throws IOException {
        List<Hashtag> hashtags = hashtagService.createHashtagsFromUrls(request.getImageUrls());
        return HashtagResponseDTO.from(hashtags);
    }

    @GetMapping("/AllHashtag")
    public List<String> searchAllHashtag() {
        return hashtagService.searchAllHashtag();
    }
}

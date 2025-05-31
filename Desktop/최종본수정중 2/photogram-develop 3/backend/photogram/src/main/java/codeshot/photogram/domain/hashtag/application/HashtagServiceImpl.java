package codeshot.photogram.domain.hashtag.application;

import codeshot.photogram.domain.hashtag.domain.entity.Hashtag;
import codeshot.photogram.domain.hashtag.domain.repository.HashtagRepository;
import codeshot.photogram.domain.hashtag.dto.PostImageWithHashtags;
import codeshot.photogram.domain.post.domain.entity.PostImage;
import codeshot.photogram.domain.post.domain.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Service
public class HashtagServiceImpl implements HashtagService {

    private final HashtagRepository hashtagRepository;
    private final PostRepository postRepository;
    private final String token = "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";
    private final String imageModel = "google/vit-base-patch16-224";

    public HashtagServiceImpl(HashtagRepository hashtagRepository, PostRepository postRepository) {
        this.hashtagRepository = hashtagRepository;
        this.postRepository = postRepository;
    }

    @Override
    public Page<PostImageWithHashtags> searchPhotosByHashtags(List<String> hashtags, Pageable pageable) {
        List<String> cleanTags = hashtags.stream()
                .map(tag -> tag.replaceAll("#", "").trim().toLowerCase())
                .toList();

        Page<PostImage> postImages = postRepository.findByHashtags(cleanTags, cleanTags.size(), pageable);

        return postImages.map(postImage -> {
            List<String> hashtagNames = postImage.getPostImageHashtags().stream()
                    .map(pih -> pih.getHashtag().getName())
                    .toList();
            return new PostImageWithHashtags(postImage, hashtagNames);
        });
    }

    @Override
    public Page<PostImage> searchUserPhotosByHashtags(List<String> hashtags, Pageable pageable, Long memberId) {
        List<String> cleanTags = hashtags.stream()
                .map(tag -> tag.replaceAll("#", "").trim().toLowerCase())
                .toList();
        return postRepository.findByUserHashtags(cleanTags, memberId, cleanTags.size(), pageable);
    }

    @Override
    public List<String> searchAllHashtag() {
        return hashtagRepository.findAllHashtag();
    }

    /**
     * ✅ 여러 이미지 URL로부터 해시태그 생성
     */
    @Transactional
    @Override
    public List<Hashtag> createHashtagsFromUrls(List<String> imageUrls) throws IOException {
        Set<String> keywordSet = new HashSet<>();

        for (String imageUrl : imageUrls) {
            try (InputStream inputStream = new URL(imageUrl).openStream()) {
                byte[] imageBytes = inputStream.readAllBytes();
                List<String> keywords = extractLabelsFromImage(imageBytes);
                for (String keyword : keywords) {
                    keywordSet.add(keyword.replace("#", "").toLowerCase());
                }
            }
        }

        List<Hashtag> savedHashtags = new ArrayList<>();
        for (String keyword : keywordSet) {
            Hashtag hashtag = hashtagRepository.findByName(keyword)
                    .orElseGet(() -> hashtagRepository.save(Hashtag.builder().name(keyword).build()));
            savedHashtags.add(hashtag);
        }

        return savedHashtags;
    }

    /**
     * ✅ 해시태그 이름으로 해시태그 생성 또는 조회
     */
    @Override
    @Transactional
    public Hashtag createOrGet(String hashtagName) {
        String clean = hashtagName.replace("#", "").toLowerCase();
        return hashtagRepository.findByName(clean)
                .orElseGet(() -> hashtagRepository.save(Hashtag.builder().name(clean).build()));
    }

    /**
     * ✅ 이미지 바이트 배열을 통해 해시태그 후보 추출
     */
    @Override
    public List<String> extractLabelsFromImage(byte[] imageBytes) throws IOException {
        URL url = new URL("https://api-inference.huggingface.co/models/" + imageModel);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setDoOutput(true);
        conn.getOutputStream().write(imageBytes);

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            responseBuilder.append(line);
        }
        br.close();
        conn.disconnect();

        String jsonResponse = responseBuilder.toString();
        String[] parts = jsonResponse.split("\\{");
        List<String> hashtags = new ArrayList<>();

        for (String part : parts) {
            if (part.contains("\"label\"")) {
                int start = part.indexOf("\"label\":\"") + 9;
                int end = part.indexOf("\"", start);
                if (start >= 9 && end > start) {
                    String label = part.substring(start, end);
                    String[] labelParts = label.split(",");
                    for (String l : labelParts) {
                        String cleaned = l.trim().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                        if (!cleaned.isEmpty() && hashtags.size() < 5) {
                            hashtags.add("#" + cleaned);
                        }
                        if (hashtags.size() >= 5) break;
                    }
                }
            }
            if (hashtags.size() >= 5) break;
        }

        return hashtags;
    }
}

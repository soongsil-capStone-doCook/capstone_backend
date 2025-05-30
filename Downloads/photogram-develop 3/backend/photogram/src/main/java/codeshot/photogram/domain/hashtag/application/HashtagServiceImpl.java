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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HashtagServiceImpl implements HashtagService {

    private final HashtagRepository hashtagRepository;
    private final PostRepository postRepository; // 이 서비스에서 PostRepository가 직접 사용되지 않는다면 제거 고려
    private final String token = "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";
    private final String imageModel = "nlpconnect/vit-gpt2-image-captioning";
    private final String textModel = "google/flan-t5-base";

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

    @Transactional
    @Override
    public List<Hashtag> createHashtagsFromUrl(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream inputStream = url.openStream()) {
            byte[] imageBytes = inputStream.readAllBytes();
            String caption = generateCaptionFromImage(imageBytes);
            if (caption.isBlank()) return Collections.emptyList();

            List<String> keywords = extractHashtagsFromCaption(caption);

            List<Hashtag> savedHashtags = new ArrayList<>();
            for (String keyword : keywords) {
                String name = keyword.replace("#", "").toLowerCase();
                Hashtag hashtag = hashtagRepository.findByName(name)
                        .orElseGet(() -> {
                            // ✅ 여기를 수정합니다.
                            Hashtag newHashtag = Hashtag.builder()
                                    .name(name) // name 필드를 설정
                                    .build();
                            return hashtagRepository.save(newHashtag); // 새로 생성된 Hashtag는 반드시 저장
                        });
                savedHashtags.add(hashtag);
            }

            return savedHashtags;
        }
    }

    // ✅ 이미지 URL로 해시태그 추출 (PostImageService에서 사용)
    @Override
    public List<String> extractHashtagsFromImage(String imageUrl) {
        try (InputStream inputStream = new URL(imageUrl).openStream()) {
            byte[] imageBytes = inputStream.readAllBytes();
            String caption = generateCaptionFromImage(imageBytes);
            if (caption.isBlank()) return Collections.emptyList();
            return extractHashtagsFromCaption(caption);
        } catch (IOException e) {
            throw new RuntimeException("AI 해시태그 생성 실패", e);
        }
    }

    // ✅ 해시태그 이름으로 해시태그 생성 또는 조회 (PostImageService에서 사용)
    @Override
    @Transactional
    public Hashtag createOrGet(String hashtagName) {
        String clean = hashtagName.replace("#", "").toLowerCase();
        return hashtagRepository.findByName(clean)
                .orElseGet(() -> {
                    // ✅ 여기를 수정합니다.
                    Hashtag newHashtag = Hashtag.builder()
                            .name(clean) // name 필드를 설정
                            .build();
                    return hashtagRepository.save(newHashtag); // 새로 생성된 Hashtag는 반드시 저장
                });
    }

    private String generateCaptionFromImage(byte[] imageBytes) throws IOException {
        URL url = new URL("https://api-inference.huggingface.co/models/" + imageModel);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setDoOutput(true);
        conn.getOutputStream().write(imageBytes);

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response = br.lines().reduce("", String::concat);
        br.close();
        conn.disconnect();

        Matcher matcher = Pattern.compile("\"generated_text\"\\s*:\\s*\"([^\"]+)\"").matcher(response);
        return matcher.find() ? matcher.group(1) : "";
    }

    private List<String> extractHashtagsFromCaption(String caption) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL("https://api-inference.huggingface.co/models/" + textModel).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String body = "{\"inputs\": \"Extract keywords: " + caption + "\"}";
        conn.getOutputStream().write(body.getBytes());

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response = br.lines().reduce("", String::concat);
        br.close();
        conn.disconnect();

        Matcher matcher = Pattern.compile("\"generated_text\"\\s*:\\s*\"([^\"]+)\"").matcher(response);
        if (!matcher.find()) return Collections.emptyList();

        String rawKeywords = matcher.group(1);
        String[] tokens = rawKeywords.split("\\s+");

        // 불용어 목록을 미리 정의해두면 매번 생성하지 않아 성능 향상
        Set<String> stopwords = Set.of("in", "on", "at", "a", "the", "of", "to", "with", "and", "for", "by", "an", "from", "is", "are");

        List<String> hashtags = new ArrayList<>();
        for (String token : tokens) {
            String clean = token.toLowerCase().replaceAll("[^a-z]", "");
            if (!clean.isBlank() && !stopwords.contains(clean)) {
                hashtags.add(clean);
            }
        }
        return hashtags;
    }
}
package codeshot.photogram.domain.post.application;

import codeshot.photogram.domain.post.domain.entity.Post;
import codeshot.photogram.domain.post.dto.request.PostCreateRequest;
import codeshot.photogram.domain.post.dto.request.UpdatePostRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostService {
    Post getPostById(Long id);
    Page<Post> getAllPosts(Pageable pageable);
    Post createPost(Post post);
    Post updatePost(Long id, UpdatePostRequest request, Long memberId);
    void deletePost(Long id, Long memberId);
    Page<Post> getPostsByTags(Long userId, List<String> tags, long size, Pageable pageable);
    Page<Post> getAllPost(Pageable pageable); // 현재 사용되지 않음

    Post createPostWithImageUrls(PostCreateRequest request, Long memberId); // 기존 메서드도 인터페이스에 선언

    // 새로 추가할 메서드
    List<Post> getAllPostsNoPaging();
    List<Post> getPostsByArea(String area);
    List<Post> getUserPostsByArea(String area, Long memberId);

}
package codeshot.photogram.domain.post.application;

import codeshot.photogram.domain.post.domain.entity.PostImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostImageService {

    PostImage uploadPostImageWithHashtags(List<MultipartFile> files, Long postId);
    PostImage uploadPostImageWithHashtagsWithoutPost(List<MultipartFile> files);

}

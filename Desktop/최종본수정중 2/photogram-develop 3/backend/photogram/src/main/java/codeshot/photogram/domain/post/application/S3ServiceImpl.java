package codeshot.photogram.domain.post.application;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class S3ServiceImpl implements S3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private final AmazonS3 amazonS3;

    public S3ServiceImpl(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files) {
        List<String> imageUrls = new ArrayList<>(); // 업로드된 URL들을 저장할 리스트

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue; // 빈 파일은 건너뜁니다.
            }
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            try {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(file.getSize());
                metadata.setContentType(file.getContentType());
                amazonS3.putObject(bucket, fileName, file.getInputStream(), metadata);

                String fileUrl = amazonS3.getUrl(bucket, fileName).toString();
                imageUrls.add(fileUrl); // 각 파일의 URL을 리스트에 추가
            } catch (IOException e) {
                // 특정 파일 업로드 실패 시 로깅하고 다음 파일로 진행하거나, 전체 실패로 처리할 수 있습니다.
                System.err.println("Failed to upload file " + file.getOriginalFilename() + " to S3: " + e.getMessage());
                // throw new RuntimeException("S3 업로드 실패", e); // 전체 실패를 원하면 주석 해제
            }
        }
        return imageUrls; // 모든 업로드된 URL 리스트 반환
    }
}


    /*
    @Override
    public String uploadFile(MultipartFile file) {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        try {
            // S3에 파일 업로드
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            amazonS3.putObject(bucket, fileName, file.getInputStream(), metadata);

            // Presigned URL 생성 (10분 유효)
            Date expiration = new Date(System.currentTimeMillis() + 1000 * 60 * 10); // 10분
            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(bucket, fileName)
                            .withMethod(HttpMethod.GET)
                            .withExpiration(expiration);
            URL presignedUrl = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);

            return presignedUrl.toString();
        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }

     */



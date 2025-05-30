package codeshot.photogram.domain.hashtag.dto;

import codeshot.photogram.domain.hashtag.domain.entity.Hashtag;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class HashtagResponseDTO {

    private Long id;
    private String name;

    public static HashtagResponseDTO from(Hashtag hashtag) {
        return new HashtagResponseDTO(hashtag.getId(), hashtag.getName());
    }

    public static List<HashtagResponseDTO> from(List<Hashtag> hashtags) {
        return hashtags.stream()
                .map(HashtagResponseDTO::from)
                .toList();
    }
}

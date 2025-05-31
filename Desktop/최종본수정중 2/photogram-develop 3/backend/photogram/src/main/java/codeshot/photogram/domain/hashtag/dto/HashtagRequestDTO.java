package codeshot.photogram.domain.hashtag.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class HashtagRequestDTO {

    private List<String> ImageUrls;
    private String hashtag;
    private List<String> hashtags;
}

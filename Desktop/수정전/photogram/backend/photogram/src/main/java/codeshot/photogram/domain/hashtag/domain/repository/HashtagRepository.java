package codeshot.photogram.domain.hashtag.domain.repository;

import codeshot.photogram.domain.hashtag.domain.entity.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    Optional<Hashtag> findByName(String name);

    @Query("SELECT h.name FROM Hashtag h")
    List<String> findAllHashtag();

}


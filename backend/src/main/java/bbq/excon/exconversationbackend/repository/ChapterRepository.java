package bbq.excon.exconversationbackend.repository;

import bbq.excon.exconversationbackend.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    Optional<Chapter> findByChapterIndex(Integer chapterIndex);
    Optional<Chapter> findByChapterName(String chapterName);
    List<Chapter> findByCreatedByNameOrderByChapterIndexAsc(String createdByName);
}


package bbq.excon.exconversationbackend.repository;

import bbq.excon.exconversationbackend.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByChapterId(Long chapterId);
    List<Question> findByIsActiveTrue();
    List<Question> findByChapterIdAndIsActiveTrue(Long chapterId);
    
    @Query("SELECT q FROM Question q WHERE q.chapter.id = :chapterId AND q.isActive = true")
    List<Question> findActiveQuestionsByChapter(@Param("chapterId") Long chapterId);
    
    // Pagination methods
    Page<Question> findByIsActiveTrue(Pageable pageable);
    Page<Question> findByChapterIdAndIsActiveTrue(Long chapterId, Pageable pageable);
}


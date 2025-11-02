package bbq.excon.exconversationbackend.repository;

import bbq.excon.exconversationbackend.entity.QuestionVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionVersionRepository extends JpaRepository<QuestionVersion, Long> {
    List<QuestionVersion> findByQuestionId(Long questionId);
    Optional<QuestionVersion> findByQuestionIdAndIsPublishedTrue(Long questionId);
    List<QuestionVersion> findByIsPublishedTrue();
}


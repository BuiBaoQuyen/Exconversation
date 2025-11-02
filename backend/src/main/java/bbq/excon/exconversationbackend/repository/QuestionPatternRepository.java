package bbq.excon.exconversationbackend.repository;

import bbq.excon.exconversationbackend.entity.QuestionPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionPatternRepository extends JpaRepository<QuestionPattern, Long> {
    List<QuestionPattern> findByIsActiveTrue();
    Optional<QuestionPattern> findByPatternName(String patternName);
}


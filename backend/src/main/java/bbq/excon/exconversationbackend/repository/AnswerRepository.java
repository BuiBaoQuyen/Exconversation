package bbq.excon.exconversationbackend.repository;

import bbq.excon.exconversationbackend.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByQuestionVersionId(Long questionVersionId);
    List<Answer> findByQuestionVersionIdOrderByOrderLabel(Long questionVersionId);
    List<Answer> findByQuestionVersionIdAndIsCorrectTrue(Long questionVersionId);
}


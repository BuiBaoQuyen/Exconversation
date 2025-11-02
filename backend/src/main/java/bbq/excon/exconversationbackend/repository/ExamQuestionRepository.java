package bbq.excon.exconversationbackend.repository;

import bbq.excon.exconversationbackend.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
    List<ExamQuestion> findByExamIdOrderByOrderNumber(Long examId);
    void deleteByExamId(Long examId);
}


package bbq.excon.exconversationbackend.repository;

import bbq.excon.exconversationbackend.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByBlueprintId(Long blueprintId);
}


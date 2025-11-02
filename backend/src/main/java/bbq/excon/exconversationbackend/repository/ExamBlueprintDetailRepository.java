package bbq.excon.exconversationbackend.repository;

import bbq.excon.exconversationbackend.entity.ExamBlueprintDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamBlueprintDetailRepository extends JpaRepository<ExamBlueprintDetail, Long> {
    List<ExamBlueprintDetail> findByExamBlueprintId(Long examBlueprintId);
    void deleteByExamBlueprintId(Long examBlueprintId);
}


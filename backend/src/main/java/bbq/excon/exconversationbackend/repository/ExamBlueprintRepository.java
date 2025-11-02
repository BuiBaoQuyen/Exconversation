package bbq.excon.exconversationbackend.repository;

import bbq.excon.exconversationbackend.entity.ExamBlueprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamBlueprintRepository extends JpaRepository<ExamBlueprint, Long> {
}


package bbq.excon.exconversationbackend.repository;

import bbq.excon.exconversationbackend.entity.ActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionLogRepository extends JpaRepository<ActionLog, Long> {
    List<ActionLog> findByActionType(String actionType);
    List<ActionLog> findByReferenceTableAndReferenceId(String referenceTable, Long referenceId);
}


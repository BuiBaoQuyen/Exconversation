package bbq.excon.exconversationbackend.repository;

import bbq.excon.exconversationbackend.entity.Upload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadRepository extends JpaRepository<Upload, Long> {
    List<Upload> findByStatus(String status);
    List<Upload> findByUploadedByName(String uploadedByName);
}


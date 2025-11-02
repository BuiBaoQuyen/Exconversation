package bbq.excon.exconversationbackend.repository;

import bbq.excon.exconversationbackend.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByQuestionVersionId(Long questionVersionId);
}


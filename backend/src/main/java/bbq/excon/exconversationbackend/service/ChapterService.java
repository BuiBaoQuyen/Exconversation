package bbq.excon.exconversationbackend.service;

import bbq.excon.exconversationbackend.entity.Chapter;
import bbq.excon.exconversationbackend.entity.Upload;
import bbq.excon.exconversationbackend.repository.ChapterRepository;
import bbq.excon.exconversationbackend.repository.UploadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChapterService {
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private UploadRepository uploadRepository;
    
    /**
     * Get chapters by upload ID (through createdByName)
     */
    public List<Chapter> getChaptersByUploadId(Long uploadId) {
        Upload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new RuntimeException("Upload not found: " + uploadId));
        
        // Find chapters created by the same user (uploadedByName)
        return chapterRepository.findByCreatedByNameOrderByChapterIndexAsc(upload.getUploadedByName());
    }
    
    /**
     * Get all chapters
     */
    public List<Chapter> getAllChapters() {
        return chapterRepository.findAll();
    }
    
    /**
     * Get chapter by ID
     */
    public Chapter getChapterById(Long id) {
        return chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + id));
    }
}


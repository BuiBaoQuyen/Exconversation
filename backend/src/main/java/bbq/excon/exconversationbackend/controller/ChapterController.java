package bbq.excon.exconversationbackend.controller;

import bbq.excon.exconversationbackend.entity.Chapter;
import bbq.excon.exconversationbackend.service.ChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
@CrossOrigin(origins = "*")
public class ChapterController {
    
    @Autowired
    private ChapterService chapterService;
    
    /**
     * Get all chapters
     */
    @GetMapping
    public ResponseEntity<List<Chapter>> getAllChapters() {
        List<Chapter> chapters = chapterService.getAllChapters();
        return ResponseEntity.ok(chapters);
    }
    
    /**
     * Get chapters by upload ID
     */
    @GetMapping("/upload/{uploadId}")
    public ResponseEntity<List<Chapter>> getChaptersByUploadId(@PathVariable Long uploadId) {
        try {
            List<Chapter> chapters = chapterService.getChaptersByUploadId(uploadId);
            return ResponseEntity.ok(chapters);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get chapter by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Chapter> getChapterById(@PathVariable Long id) {
        try {
            Chapter chapter = chapterService.getChapterById(id);
            return ResponseEntity.ok(chapter);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}


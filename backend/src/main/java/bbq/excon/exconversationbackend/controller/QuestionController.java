package bbq.excon.exconversationbackend.controller;

import bbq.excon.exconversationbackend.dto.QuestionDTO;
import bbq.excon.exconversationbackend.dto.PageResponse;
import bbq.excon.exconversationbackend.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
public class QuestionController {
    
    @Autowired
    private QuestionService questionService;
    
    /**
     * Get all questions (with pagination support)
     */
    @GetMapping
    public ResponseEntity<?> getAllQuestions(
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        // Use pagination if page/size is specified, otherwise return all (for backward compatibility)
        if (page >= 0 && size > 0) {
            Page<QuestionDTO> questionPage;
            if (chapterId != null) {
                questionPage = questionService.getQuestionsByChapterPaginated(chapterId, page, size);
            } else {
                questionPage = questionService.getAllQuestionsPaginated(page, size);
            }
            // Wrap Page trong PageResponse để tránh warning về serialize PageImpl
            return ResponseEntity.ok(new PageResponse<>(questionPage));
        } else {
            // Backward compatibility: return all questions
            List<QuestionDTO> questions;
            if (chapterId != null) {
                questions = questionService.getQuestionsByChapter(chapterId);
            } else {
                questions = questionService.getAllQuestions();
            }
            return ResponseEntity.ok(questions);
        }
    }
    
    /**
     * Get question by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuestionDTO> getQuestionById(@PathVariable Long id) {
        Optional<QuestionDTO> question = questionService.getQuestionById(id);
        return question.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Update question
     */
    @PutMapping("/{id}")
    public ResponseEntity<QuestionDTO> updateQuestion(
            @PathVariable Long id,
            @RequestBody QuestionDTO dto) {
        Optional<QuestionDTO> updated = questionService.updateQuestion(id, dto);
        return updated.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Create new question version
     */
    @PostMapping("/{id}/version")
    public ResponseEntity<?> createQuestionVersion(
            @PathVariable Long id,
            @RequestBody QuestionDTO dto) {
        try {
            Optional<?> version = questionService.createQuestionVersion(id, dto);
            return version.map(v -> ResponseEntity.status(HttpStatus.CREATED).body(v))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    /**
     * Delete question (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        boolean deleted = questionService.deleteQuestion(id);
        return deleted ? ResponseEntity.noContent().build()
                      : ResponseEntity.notFound().build();
    }
}


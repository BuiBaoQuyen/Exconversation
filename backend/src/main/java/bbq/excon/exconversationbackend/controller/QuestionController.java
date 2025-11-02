package bbq.excon.exconversationbackend.controller;

import bbq.excon.exconversationbackend.dto.QuestionDTO;
import bbq.excon.exconversationbackend.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
public class QuestionController {
    
    @Autowired
    private QuestionService questionService;
    
    /**
     * Get all questions
     */
    @GetMapping
    public ResponseEntity<List<QuestionDTO>> getAllQuestions(
            @RequestParam(required = false) Long chapterId) {
        List<QuestionDTO> questions;
        if (chapterId != null) {
            questions = questionService.getQuestionsByChapter(chapterId);
        } else {
            questions = questionService.getAllQuestions();
        }
        return ResponseEntity.ok(questions);
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


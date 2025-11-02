package bbq.excon.exconversationbackend.controller;

import bbq.excon.exconversationbackend.dto.ExamBlueprintDTO;
import bbq.excon.exconversationbackend.dto.ExamDTO;
import bbq.excon.exconversationbackend.service.ExamBlueprintService;
import bbq.excon.exconversationbackend.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ExamController {
    
    @Autowired
    private ExamBlueprintService blueprintService;
    
    @Autowired
    private ExamService examService;
    
    // ==================== Blueprint Endpoints ====================
    
    @GetMapping("/blueprints")
    public ResponseEntity<List<ExamBlueprintDTO>> getAllBlueprints() {
        List<ExamBlueprintDTO> blueprints = blueprintService.getAllBlueprints();
        return ResponseEntity.ok(blueprints);
    }
    
    @GetMapping("/blueprints/{id}")
    public ResponseEntity<ExamBlueprintDTO> getBlueprintById(@PathVariable Long id) {
        Optional<ExamBlueprintDTO> blueprint = blueprintService.getBlueprintById(id);
        return blueprint.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/blueprints")
    public ResponseEntity<?> createBlueprint(@RequestBody ExamBlueprintDTO dto) {
        try {
            ExamBlueprintDTO created = blueprintService.createBlueprint(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    @PutMapping("/blueprints/{id}")
    public ResponseEntity<?> updateBlueprint(
            @PathVariable Long id,
            @RequestBody ExamBlueprintDTO dto) {
        try {
            Optional<ExamBlueprintDTO> updated = blueprintService.updateBlueprint(id, dto);
            return updated.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    @DeleteMapping("/blueprints/{id}")
    public ResponseEntity<Void> deleteBlueprint(@PathVariable Long id) {
        boolean deleted = blueprintService.deleteBlueprint(id);
        return deleted ? ResponseEntity.noContent().build()
                      : ResponseEntity.notFound().build();
    }
    
    // ==================== Exam Endpoints ====================
    
    @GetMapping("/exams")
    public ResponseEntity<List<ExamDTO>> getAllExams() {
        List<ExamDTO> exams = examService.getAllExams();
        return ResponseEntity.ok(exams);
    }
    
    @GetMapping("/exams/{id}")
    public ResponseEntity<ExamDTO> getExamById(@PathVariable Long id) {
        Optional<ExamDTO> exam = examService.getExamById(id);
        return exam.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/exams/generate")
    public ResponseEntity<?> generateExam(
            @RequestParam Long blueprintId,
            @RequestParam(required = false) String examName,
            @RequestParam(required = false, defaultValue = "System") String createdByName) {
        try {
            ExamDTO exam = examService.generateExam(blueprintId, examName, createdByName);
            return ResponseEntity.status(HttpStatus.CREATED).body(exam);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to generate exam: " + e.getMessage() + "\"}");
        }
    }
    
    @DeleteMapping("/exams/{id}")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        boolean deleted = examService.deleteExam(id);
        return deleted ? ResponseEntity.noContent().build()
                      : ResponseEntity.notFound().build();
    }
}


package bbq.excon.exconversationbackend.controller;

import bbq.excon.exconversationbackend.dto.ExamDTO;
import bbq.excon.exconversationbackend.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ExamController {
    
    @Autowired
    private ExamService examService;
    
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
    
    @DeleteMapping("/exams/{id}")
    public ResponseEntity<Void> deleteExam(@PathVariable Long id) {
        boolean deleted = examService.deleteExam(id);
        return deleted ? ResponseEntity.noContent().build()
                      : ResponseEntity.notFound().build();
    }
}

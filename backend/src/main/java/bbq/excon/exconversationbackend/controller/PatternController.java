package bbq.excon.exconversationbackend.controller;

import bbq.excon.exconversationbackend.dto.PatternTestResult;
import bbq.excon.exconversationbackend.dto.QuestionPatternDTO;
import bbq.excon.exconversationbackend.service.PatternService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/patterns")
@CrossOrigin(origins = "*")
public class PatternController {
    
    @Autowired
    private PatternService patternService;
    
    @GetMapping
    public ResponseEntity<List<QuestionPatternDTO>> getAllPatterns(
            @RequestParam(required = false) Boolean activeOnly) {
        List<QuestionPatternDTO> patterns;
        if (activeOnly != null && activeOnly) {
            patterns = patternService.getActivePatterns();
        } else {
            patterns = patternService.getAllPatterns();
        }
        return ResponseEntity.ok(patterns);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<QuestionPatternDTO> getPatternById(@PathVariable Long id) {
        Optional<QuestionPatternDTO> pattern = patternService.getPatternById(id);
        return pattern.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<QuestionPatternDTO> createPattern(@RequestBody QuestionPatternDTO dto) {
        try {
            QuestionPatternDTO created = patternService.createPattern(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<QuestionPatternDTO> updatePattern(
            @PathVariable Long id,
            @RequestBody QuestionPatternDTO dto) {
        Optional<QuestionPatternDTO> updated = patternService.updatePattern(id, dto);
        return updated.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePattern(@PathVariable Long id) {
        boolean deleted = patternService.deletePattern(id);
        return deleted ? ResponseEntity.noContent().build() 
                      : ResponseEntity.notFound().build();
    }
    
    @PostMapping("/{id}/test")
    public ResponseEntity<PatternTestResult> testPattern(
            @PathVariable Long id,
            @RequestBody String testText) {
        PatternTestResult result = patternService.testPattern(id, testText);
        return ResponseEntity.ok(result);
    }
}


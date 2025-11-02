package bbq.excon.exconversationbackend.controller;

import bbq.excon.exconversationbackend.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/exams")
@CrossOrigin(origins = "*")
public class ExportController {
    
    @Autowired
    private ExportService exportService;
    
    /**
     * Export exam to DOCX file
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<Resource> exportExam(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean includeAnswers) {
        try {
            // Export to DOCX
            String filePath = exportService.exportExamToDOCX(id, includeAnswers);
            
            // Create resource
            Path path = Paths.get(filePath);
            Resource resource = new FileSystemResource(path.toFile());
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type
            String contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            
            // Get filename
            String filename = path.getFileName().toString();
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
    
    /**
     * Export exam with answers
     */
    @GetMapping("/{id}/export-with-answers")
    public ResponseEntity<Resource> exportExamWithAnswers(@PathVariable Long id) {
        return exportExam(id, true);
    }
}


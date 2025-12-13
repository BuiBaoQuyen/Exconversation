package bbq.excon.exconversationbackend.controller;

import bbq.excon.exconversationbackend.service.ExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);
    
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
    
    /**
     * Generate and export random exam to DOCX file
     */
    @GetMapping("/print-random")
    public ResponseEntity<Resource> printRandomExam(
            @RequestParam(required = false, defaultValue = "40") int numberOfQuestions,
            @RequestParam(required = false, defaultValue = "false") boolean includeAnswers) {
        logger.info("=== printRandomExam called ===");
        logger.info("Request parameters: numberOfQuestions={}, includeAnswers={}", numberOfQuestions, includeAnswers);
        
        try {
            logger.info("Calling exportService.exportRandomExamToDOCX...");
            String filePath = exportService.exportRandomExamToDOCX(numberOfQuestions, includeAnswers);
            logger.info("Export completed. File path: {}", filePath);
            
            // Create resource
            Path path = Paths.get(filePath);
            logger.info("Checking if file exists: {}", path.toAbsolutePath());
            Resource resource = new FileSystemResource(path.toFile());
            
            if (!resource.exists()) {
                logger.error("File does not exist: {}", path.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            logger.info("File exists. File size: {} bytes", resource.contentLength());
            
            // Determine content type
            String contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            
            // Get filename
            String filename = path.getFileName().toString();
            logger.info("Returning file: {} with content type: {}", filename, contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (RuntimeException e) {
            logger.error("RuntimeException in printRandomExam: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .build();
        } catch (Exception e) {
            logger.error("Exception in printRandomExam: {}", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}


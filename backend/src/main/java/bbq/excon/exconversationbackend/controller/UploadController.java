package bbq.excon.exconversationbackend.controller;

import bbq.excon.exconversationbackend.entity.Upload;
import bbq.excon.exconversationbackend.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class UploadController {
    
    @Autowired
    private UploadService uploadService;
    
    @Autowired
    private bbq.excon.exconversationbackend.parser.DocumentParserService documentParserService;
    
    /**
     * Upload file DOCX và tự động parse bằng Hybrid approach (Fast + AI)
     */
    @PostMapping
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "System") String uploadedByName) {
        try {
            Upload upload = uploadService.uploadFile(file, uploadedByName);
            return ResponseEntity.status(HttpStatus.CREATED).body(upload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to upload file: " + e.getMessage() + "\"}");
        }
    }
    
    /**
     * Get all uploads
     */
    @GetMapping
    public ResponseEntity<List<Upload>> getAllUploads() {
        List<Upload> uploads = uploadService.getAllUploads();
        return ResponseEntity.ok(uploads);
    }
    
    /**
     * Get upload by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Upload> getUploadById(@PathVariable Long id) {
        try {
            Upload upload = uploadService.getUploadById(id);
            return ResponseEntity.ok(upload);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get uploads by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Upload>> getUploadsByStatus(@PathVariable String status) {
        List<Upload> uploads = uploadService.getUploadsByStatus(status);
        return ResponseEntity.ok(uploads);
    }
    
    /**
     * Parse lại DOCX file bằng DocumentParserService mới (nếu cần parse lại)
     * Async processing - returns immediately
     */
    @PostMapping("/{id}/parse")
    public ResponseEntity<?> parseDocument(@PathVariable Long id) {
        try {
            // Start async parsing with new DocumentParserService
            documentParserService.parseDocument(id);
            
            // Return immediately
            return ResponseEntity.accepted()
                    .body("{\"message\": \"Document parsing started. Check status via GET /api/upload/" + id + "\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to start parsing: " + e.getMessage() + "\"}");
        }
    }
    
    /**
     * Delete upload
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUpload(@PathVariable Long id) {
        boolean deleted = uploadService.deleteUpload(id);
        return deleted ? ResponseEntity.noContent().build() 
                      : ResponseEntity.notFound().build();
    }
}


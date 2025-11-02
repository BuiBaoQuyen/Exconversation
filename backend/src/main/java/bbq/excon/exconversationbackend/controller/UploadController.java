package bbq.excon.exconversationbackend.controller;

import bbq.excon.exconversationbackend.dto.ParseResult;
import bbq.excon.exconversationbackend.entity.Upload;
import bbq.excon.exconversationbackend.service.ParseService;
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
    private ParseService parseService;
    
    /**
     * Upload file DOCX
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
     * Parse DOCX file với pattern đã chọn
     */
    @PostMapping("/{id}/parse")
    public ResponseEntity<ParseResult> parseDocument(
            @PathVariable Long id,
            @RequestParam Long patternId) {
        try {
            ParseResult result = parseService.parseDocument(id, patternId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            ParseResult errorResult = new ParseResult();
            errorResult.setSuccess(false);
            errorResult.setMessage("Error parsing document: " + e.getMessage());
            errorResult.addError(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
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


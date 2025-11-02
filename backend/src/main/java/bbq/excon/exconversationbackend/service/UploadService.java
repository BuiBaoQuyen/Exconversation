package bbq.excon.exconversationbackend.service;

import bbq.excon.exconversationbackend.entity.Upload;
import bbq.excon.exconversationbackend.repository.UploadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
public class UploadService {
    
    @Autowired
    private UploadRepository uploadRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * Upload và lưu file DOCX
     */
    @Transactional
    public Upload uploadFile(MultipartFile file, String uploadedByName) throws Exception {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".docx")) {
            throw new IllegalArgumentException("Only .docx files are supported");
        }
        
        // Initialize directories
        fileStorageService.initializeDirectories();
        
        // Store file
        String filePath = fileStorageService.storeFile(file);
        
        // Save upload record
        Upload upload = new Upload();
        upload.setFileName(originalFilename);
        upload.setFilePath(filePath);
        upload.setUploadedByName(uploadedByName);
        upload.setStatus("pending");
        upload.setUploadDate(LocalDateTime.now());
        
        return uploadRepository.save(upload);
    }
    
    /**
     * Get upload by ID
     */
    public Upload getUploadById(Long id) {
        return uploadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Upload not found: " + id));
    }
    
    /**
     * Get all uploads
     */
    public java.util.List<Upload> getAllUploads() {
        return uploadRepository.findAll();
    }
    
    /**
     * Get uploads by status
     */
    public java.util.List<Upload> getUploadsByStatus(String status) {
        return uploadRepository.findByStatus(status);
    }
    
    /**
     * Delete upload
     */
    @Transactional
    public boolean deleteUpload(Long id) {
        Upload upload = uploadRepository.findById(id).orElse(null);
        if (upload != null) {
            try {
                // Delete file
                fileStorageService.deleteFile(upload.getFilePath());
                // Delete record
                uploadRepository.deleteById(id);
                return true;
            } catch (Exception e) {
                throw new RuntimeException("Error deleting upload: " + e.getMessage(), e);
            }
        }
        return false;
    }
}


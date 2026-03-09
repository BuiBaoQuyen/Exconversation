package bbq.excon.exconversationbackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
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
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {
    
    @Value("${file.images-dir}")
    private String imagesDir;
    
    /**
     * Serve image file by path from URL
     * Example: /api/images/19/image123.png
     */
    @GetMapping("/**")
    public ResponseEntity<Resource> getImage(HttpServletRequest request) {
        try {
            // Get the full request URI
            String requestURI = request.getRequestURI();
            
            // Remove /api/images prefix
            String imagePath = requestURI;
            if (imagePath.startsWith("/api/images/")) {
                imagePath = imagePath.substring("/api/images/".length());
            } else if (imagePath.startsWith("api/images/")) {
                imagePath = imagePath.substring("api/images/".length());
            }
            
            if (imagePath.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Try as relative path first (relative to images directory)
            Path filePath = Paths.get(imagesDir, imagePath);
            
            // If not found, try as absolute path (path from database might be absolute)
            if (!Files.exists(filePath)) {
                filePath = Paths.get(imagePath);
            }
            
            // If still not found and path contains separators, try constructing from imagesDir
            if (!Files.exists(filePath) && imagePath.contains(File.separator)) {
                // Extract relative part if it's an absolute path that starts with imagesDir
                String imagesDirNormalized = Paths.get(imagesDir).normalize().toString();
                if (imagePath.startsWith(imagesDirNormalized)) {
                    String relativePath = imagePath.substring(imagesDirNormalized.length());
                    if (relativePath.startsWith(File.separator)) {
                        relativePath = relativePath.substring(1);
                    }
                    filePath = Paths.get(imagesDir, relativePath);
                }
            }
            
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(filePath.toFile());
            
            // Determine content type based on file extension
            String contentType = determineContentType(filePath.toString());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName().toString() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get image by ID (from database path)
     */
    @GetMapping("/by-path")
    public ResponseEntity<Resource> getImageByPath(@RequestParam String imagePath) {
        try {
            Path filePath;
            
            // If path is absolute, use it directly
            if (Paths.get(imagePath).isAbsolute()) {
                filePath = Paths.get(imagePath);
            } else {
                // Otherwise, treat as relative to images directory
                filePath = Paths.get(imagesDir, imagePath);
            }
            
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(filePath.toFile());
            
            // Determine content type
            String contentType = determineContentType(filePath.toString());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName().toString() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    
    /**
     * Determine content type from file extension
     */
    private String determineContentType(String filePath) {
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerPath.endsWith(".png")) {
            return "image/png";
        } else if (lowerPath.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerPath.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerPath.endsWith(".tiff") || lowerPath.endsWith(".tif")) {
            return "image/tiff";
        } else {
            return "application/octet-stream";
        }
    }
}


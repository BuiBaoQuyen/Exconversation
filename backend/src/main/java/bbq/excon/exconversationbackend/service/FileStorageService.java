package bbq.excon.exconversationbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    
    @Value("${file.upload-dir}")
    private String uploadDir;
    
    @Value("${file.images-dir}")
    private String imagesDir;
    
    @Value("${file.export-dir}")
    private String exportDir;
    
    public String storeFile(MultipartFile file) throws IOException {
        // Tạo thư mục nếu chưa tồn tại
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;
        
        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return filePath.toString();
    }
    
    public String storeImage(MultipartFile image, Long questionId) throws IOException {
        // Tạo thư mục cho question nếu chưa tồn tại
        Path questionImagePath = Paths.get(imagesDir, questionId.toString());
        if (!Files.exists(questionImagePath)) {
            Files.createDirectories(questionImagePath);
        }
        
        // Generate unique filename
        String originalFilename = image.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;
        
        // Save image
        Path imagePath = questionImagePath.resolve(filename);
        Files.copy(image.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
        
        return imagePath.toString();
    }
    
    public String storeImageFromBytes(byte[] imageData, String imageName, Long questionId) throws IOException {
        // Tạo thư mục cho question nếu chưa tồn tại
        Path questionImagePath = Paths.get(imagesDir, questionId.toString());
        if (!Files.exists(questionImagePath)) {
            Files.createDirectories(questionImagePath);
        }
        
        // Generate unique filename
        String extension = "";
        if (imageName != null && imageName.contains(".")) {
            extension = imageName.substring(imageName.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;
        
        // Save image
        Path imagePath = questionImagePath.resolve(filename);
        Files.write(imagePath, imageData);
        
        return imagePath.toString();
    }
    
    public Path loadFile(String filePath) {
        return Paths.get(filePath);
    }
    
    public void deleteFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }
    
    public void initializeDirectories() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
        Files.createDirectories(Paths.get(imagesDir));
        Files.createDirectories(Paths.get(exportDir));
    }
}


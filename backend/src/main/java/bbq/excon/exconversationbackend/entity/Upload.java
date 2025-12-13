package bbq.excon.exconversationbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Upload")
public class Upload {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    
    @Column(name = "File_Name", nullable = false, length = 512, columnDefinition = "NVARCHAR(512)")
    private String fileName;
    
    @Column(name = "File_Path", nullable = false, length = 1024, columnDefinition = "NVARCHAR(1024)")
    private String filePath;
    
    @Column(name = "Uploaded_By_Name", length = 255, columnDefinition = "NVARCHAR(255)")
    private String uploadedByName;
    
    @Column(name = "Upload_Date")
    private LocalDateTime uploadDate;
    
    @Column(name = "Status", length = 50, columnDefinition = "NVARCHAR(50)")
    private String status = "pending"; // pending, parsed, error
    
    @Column(name = "Note", columnDefinition = "NVARCHAR(MAX)")
    private String note;
    
    // Constructors
    public Upload() {
        this.uploadDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public String getUploadedByName() {
        return uploadedByName;
    }
    
    public void setUploadedByName(String uploadedByName) {
        this.uploadedByName = uploadedByName;
    }
    
    public LocalDateTime getUploadDate() {
        return uploadDate;
    }
    
    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
}


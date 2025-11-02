package bbq.excon.exconversationbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Image")
public class Image {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Question_Version_Id")
    private QuestionVersion questionVersion;
    
    @Column(name = "Image_Path", nullable = false, length = 1024)
    private String imagePath;
    
    @Column(name = "Description", length = 255)
    private String description;
    
    @Column(name = "Created_At")
    private LocalDateTime createdAt;
    
    // Constructors
    public Image() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public QuestionVersion getQuestionVersion() {
        return questionVersion;
    }
    
    public void setQuestionVersion(QuestionVersion questionVersion) {
        this.questionVersion = questionVersion;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


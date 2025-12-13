package bbq.excon.exconversationbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Question")
public class Question {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Chapter_Id", nullable = false)
    private Chapter chapter;
    
    @Column(name = "Type", length = 50, columnDefinition = "NVARCHAR(50)")
    private String type = "Trắc nghiệm"; // MCQ, Essay, FillInBlank
    
    @Column(name = "Created_By_Name", length = 255, columnDefinition = "NVARCHAR(255)")
    private String createdByName;
    
    @Column(name = "Created_At")
    private LocalDateTime createdAt;
    
    @Column(name = "Is_Active")
    private Boolean isActive = true;
    
    // Constructors
    public Question() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Chapter getChapter() {
        return chapter;
    }
    
    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getCreatedByName() {
        return createdByName;
    }
    
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}


package bbq.excon.exconversationbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Chapter")
public class Chapter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    
    @Column(name = "Chapter_Index", nullable = false)
    private Integer chapterIndex;
    
    @Column(name = "Chapter_Name", nullable = false, length = 255)
    private String chapterName;
    
    @Column(name = "Created_By_Name", length = 255)
    private String createdByName;
    
    @Column(name = "Created_At")
    private LocalDateTime createdAt;
    
    // Constructors
    public Chapter() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getChapterIndex() {
        return chapterIndex;
    }
    
    public void setChapterIndex(Integer chapterIndex) {
        this.chapterIndex = chapterIndex;
    }
    
    public String getChapterName() {
        return chapterName;
    }
    
    public void setChapterName(String chapterName) {
        this.chapterName = chapterName;
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
}


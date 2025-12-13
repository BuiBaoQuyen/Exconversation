package bbq.excon.exconversationbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "QuestionVersion")
public class QuestionVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Question_Id", nullable = false)
    private Question question;
    
    @Column(name = "Version_Number", nullable = false)
    private Integer versionNumber = 1;
    
    @Column(name = "Title", length = 500, columnDefinition = "NVARCHAR(500)")
    private String title;
    
    @Column(name = "Content_OMML", columnDefinition = "NVARCHAR(MAX)")
    private String contentOmml; // OMML + text

    @Column(name = "Content_LaTeX", columnDefinition = "NVARCHAR(MAX)")
    private String contentLatex; // LaTeX + text
    
    @Column(name = "Created_At")
    private LocalDateTime createdAt;
    
    @Column(name = "Created_By_Name", length = 255, columnDefinition = "NVARCHAR(255)")
    private String createdByName;
    
    @Column(name = "Is_Published")
    private Boolean isPublished = false;
    
    // Constructors
    public QuestionVersion() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Question getQuestion() {
        return question;
    }
    
    public void setQuestion(Question question) {
        this.question = question;
    }
    
    public Integer getVersionNumber() {
        return versionNumber;
    }
    
    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContentOmml() { return contentOmml; }
    public void setContentOmml(String contentOmml) { this.contentOmml = contentOmml; }

    public String getContentLatex() { return contentLatex; }
    public void setContentLatex(String contentLatex) { this.contentLatex = contentLatex; }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getCreatedByName() {
        return createdByName;
    }
    
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
    
    public Boolean getIsPublished() {
        return isPublished;
    }
    
    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }
}


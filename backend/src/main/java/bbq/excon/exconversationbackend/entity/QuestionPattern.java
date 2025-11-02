package bbq.excon.exconversationbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "QuestionPattern")
public class QuestionPattern {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    
    @Column(name = "Pattern_Name", nullable = false, length = 255)
    private String patternName;
    
    @Column(name = "Question_Pattern", nullable = false, length = 500)
    private String questionPattern; // Regex để detect câu hỏi
    
    @Column(name = "Answer_Pattern", nullable = false, length = 500)
    private String answerPattern; // Regex để detect đáp án
    
    @Column(name = "Chapter_Detector", length = 500)
    private String chapterDetector; // Regex để detect chương
    
    @Lob
    @Column(name = "Example_Text")
    private String exampleText; // Sample text để test
    
    @Column(name = "Created_At")
    private LocalDateTime createdAt;
    
    @Column(name = "Updated_At")
    private LocalDateTime updatedAt;
    
    @Column(name = "Is_Active")
    private Boolean isActive = true;
    
    @Column(name = "Created_By_Name", length = 255)
    private String createdByName;
    
    // Constructors
    public QuestionPattern() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPatternName() {
        return patternName;
    }
    
    public void setPatternName(String patternName) {
        this.patternName = patternName;
    }
    
    public String getQuestionPattern() {
        return questionPattern;
    }
    
    public void setQuestionPattern(String questionPattern) {
        this.questionPattern = questionPattern;
    }
    
    public String getAnswerPattern() {
        return answerPattern;
    }
    
    public void setAnswerPattern(String answerPattern) {
        this.answerPattern = answerPattern;
    }
    
    public String getChapterDetector() {
        return chapterDetector;
    }
    
    public void setChapterDetector(String chapterDetector) {
        this.chapterDetector = chapterDetector;
    }
    
    public String getExampleText() {
        return exampleText;
    }
    
    public void setExampleText(String exampleText) {
        this.exampleText = exampleText;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getCreatedByName() {
        return createdByName;
    }
    
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
}


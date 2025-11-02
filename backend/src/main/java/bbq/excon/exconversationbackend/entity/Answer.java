package bbq.excon.exconversationbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Answer")
public class Answer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Question_Version_Id", nullable = false)
    private QuestionVersion questionVersion;
    
    @Column(name = "Order_Label", length = 10)
    private String orderLabel; // A, B, C, D
    
    @Lob
    @Column(name = "Content", nullable = false)
    private String content;
    
    @Column(name = "Is_Correct")
    private Boolean isCorrect = false;
    
    @Column(name = "Created_At")
    private LocalDateTime createdAt;
    
    // Constructors
    public Answer() {
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
    
    public String getOrderLabel() {
        return orderLabel;
    }
    
    public void setOrderLabel(String orderLabel) {
        this.orderLabel = orderLabel;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Boolean getIsCorrect() {
        return isCorrect;
    }
    
    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


package bbq.excon.exconversationbackend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ExamQuestion")
public class ExamQuestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Exam_Id", nullable = false)
    private Exam exam;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Question_Version_Id", nullable = false)
    private QuestionVersion questionVersion;
    
    @Column(name = "Order_Number", nullable = false)
    private Integer orderNumber;
    
    // Constructors
    public ExamQuestion() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Exam getExam() {
        return exam;
    }
    
    public void setExam(Exam exam) {
        this.exam = exam;
    }
    
    public QuestionVersion getQuestionVersion() {
        return questionVersion;
    }
    
    public void setQuestionVersion(QuestionVersion questionVersion) {
        this.questionVersion = questionVersion;
    }
    
    public Integer getOrderNumber() {
        return orderNumber;
    }
    
    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }
}


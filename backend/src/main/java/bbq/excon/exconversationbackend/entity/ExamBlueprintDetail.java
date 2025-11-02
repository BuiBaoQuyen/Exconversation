package bbq.excon.exconversationbackend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ExamBlueprintDetail")
public class ExamBlueprintDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Exam_Blueprint_Id", nullable = false)
    private ExamBlueprint examBlueprint;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Chapter_Id", nullable = false)
    private Chapter chapter;
    
    @Column(name = "Percentage", nullable = false)
    private Double percentage;
    
    // Constructors
    public ExamBlueprintDetail() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ExamBlueprint getExamBlueprint() {
        return examBlueprint;
    }
    
    public void setExamBlueprint(ExamBlueprint examBlueprint) {
        this.examBlueprint = examBlueprint;
    }
    
    public Chapter getChapter() {
        return chapter;
    }
    
    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }
    
    public Double getPercentage() {
        return percentage;
    }
    
    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }
}


package bbq.excon.exconversationbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ExamBlueprint")
public class ExamBlueprint {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    
    @Column(name = "Name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "Total_Questions", nullable = false)
    private Integer totalQuestions;
    
    @Lob
    @Column(name = "Description")
    private String description;
    
    @Column(name = "Created_At")
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "examBlueprint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExamBlueprintDetail> details = new ArrayList<>();
    
    // Constructors
    public ExamBlueprint() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Integer getTotalQuestions() {
        return totalQuestions;
    }
    
    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
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
    
    public List<ExamBlueprintDetail> getDetails() {
        return details;
    }
    
    public void setDetails(List<ExamBlueprintDetail> details) {
        this.details = details;
    }
}


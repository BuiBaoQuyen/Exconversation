package bbq.excon.exconversationbackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ActionLog")
public class ActionLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;
    
    @Column(name = "Action_Type", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String actionType; // UPLOAD, GENERATE_EXAM, etc.
    
    @Column(name = "Reference_Id")
    private Long referenceId;
    
    @Column(name = "Reference_Table", length = 100, columnDefinition = "NVARCHAR(100)")
    private String referenceTable; // Upload, Exam
    
    @Column(name = "Actor_Name", length = 255, columnDefinition = "NVARCHAR(255)")
    private String actorName;
    
    @Column(name = "Created_At")
    private LocalDateTime createdAt;
    
    @Column(name = "Message", columnDefinition = "NVARCHAR(MAX)")
    private String message;
    
    // Constructors
    public ActionLog() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    
    public Long getReferenceId() {
        return referenceId;
    }
    
    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }
    
    public String getReferenceTable() {
        return referenceTable;
    }

    public void setReferenceTable(String referenceTable) {
        this.referenceTable = referenceTable;
    }
    
    public String getActorName() {
        return actorName;
    }
    
    public void setActorName(String actorName) {
        this.actorName = actorName;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}


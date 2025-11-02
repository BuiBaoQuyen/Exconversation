package bbq.excon.exconversationbackend.dto;

import java.util.List;

public class ExamDTO {
    private Long id;
    private Long blueprintId;
    private String blueprintName;
    private String name;
    private String note;
    private List<ExamQuestionDTO> questions;
    
    // Constructors
    public ExamDTO() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getBlueprintId() {
        return blueprintId;
    }
    
    public void setBlueprintId(Long blueprintId) {
        this.blueprintId = blueprintId;
    }
    
    public String getBlueprintName() {
        return blueprintName;
    }
    
    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public List<ExamQuestionDTO> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<ExamQuestionDTO> questions) {
        this.questions = questions;
    }
    
    public static class ExamQuestionDTO {
        private Long id;
        private Integer orderNumber;
        private QuestionDTO question;
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public Integer getOrderNumber() {
            return orderNumber;
        }
        
        public void setOrderNumber(Integer orderNumber) {
            this.orderNumber = orderNumber;
        }
        
        public QuestionDTO getQuestion() {
            return question;
        }
        
        public void setQuestion(QuestionDTO question) {
            this.question = question;
        }
    }
}


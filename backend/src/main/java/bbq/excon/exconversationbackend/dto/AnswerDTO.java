package bbq.excon.exconversationbackend.dto;

public class AnswerDTO {
    private Long id;
    private String orderLabel;
    private String contentOmml;
    private String contentMathml;
    private Boolean isCorrect;
    
    // Constructors
    public AnswerDTO() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getOrderLabel() {
        return orderLabel;
    }
    
    public void setOrderLabel(String orderLabel) {
        this.orderLabel = orderLabel;
    }
    
    public String getContentOmml() { return contentOmml; }
    public void setContentOmml(String contentOmml) { this.contentOmml = contentOmml; }

    public String getContentMathml() { return contentMathml; }
    public void setContentMathml(String contentMathml) { this.contentMathml = contentMathml; }
    
    public Boolean getIsCorrect() {
        return isCorrect;
    }
    
    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
}


package bbq.excon.exconversationbackend.dto;

public class AnswerDTO {
    private Long id;
    private String orderLabel;
    private String content;
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
}


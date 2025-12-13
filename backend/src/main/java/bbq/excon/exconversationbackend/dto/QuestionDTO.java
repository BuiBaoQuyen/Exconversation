package bbq.excon.exconversationbackend.dto;

import java.util.List;

public class QuestionDTO {
    private Long id;
    private Long chapterId;
    private String chapterName;
    private String type;
    private Boolean isActive;
    private Long currentVersionId;
    private String title;
    private String contentOmml;   // text + OMML
    private String contentMathml;  // text + MathML
    private List<AnswerDTO> answers;
    private List<ImageDTO> images;
    
    // Constructors
    public QuestionDTO() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getChapterId() {
        return chapterId;
    }
    
    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }
    
    public String getChapterName() {
        return chapterName;
    }
    
    public void setChapterName(String chapterName) {
        this.chapterName = chapterName;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Long getCurrentVersionId() {
        return currentVersionId;
    }
    
    public void setCurrentVersionId(Long currentVersionId) {
        this.currentVersionId = currentVersionId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContentOmml() { return contentOmml; }
    public void setContentOmml(String contentOmml) { this.contentOmml = contentOmml; }

    public String getContentMathml() { return contentMathml; }
    public void setContentMathml(String contentMathml) { this.contentMathml = contentMathml; }
    
    public List<AnswerDTO> getAnswers() {
        return answers;
    }
    
    public void setAnswers(List<AnswerDTO> answers) {
        this.answers = answers;
    }
    
    public List<ImageDTO> getImages() {
        return images;
    }
    
    public void setImages(List<ImageDTO> images) {
        this.images = images;
    }
}


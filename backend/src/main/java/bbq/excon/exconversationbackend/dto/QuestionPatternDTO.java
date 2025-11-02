package bbq.excon.exconversationbackend.dto;

public class QuestionPatternDTO {
    private Long id;
    private String patternName;
    private String questionPattern;
    private String answerPattern;
    private String chapterDetector;
    private String exampleText;
    private Boolean isActive;
    
    // Constructors
    public QuestionPatternDTO() {
    }
    
    public QuestionPatternDTO(Long id, String patternName, String questionPattern, 
                             String answerPattern, String chapterDetector, 
                             String exampleText, Boolean isActive) {
        this.id = id;
        this.patternName = patternName;
        this.questionPattern = questionPattern;
        this.answerPattern = answerPattern;
        this.chapterDetector = chapterDetector;
        this.exampleText = exampleText;
        this.isActive = isActive;
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
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}


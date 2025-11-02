package bbq.excon.exconversationbackend.dto;

import java.util.List;

public class PatternTestResult {
    private boolean success;
    private String message;
    private List<String> matchedQuestions;
    private List<String> matchedAnswers;
    private List<String> matchedChapters;
    
    // Constructors
    public PatternTestResult() {
    }
    
    public PatternTestResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<String> getMatchedQuestions() {
        return matchedQuestions;
    }
    
    public void setMatchedQuestions(List<String> matchedQuestions) {
        this.matchedQuestions = matchedQuestions;
    }
    
    public List<String> getMatchedAnswers() {
        return matchedAnswers;
    }
    
    public void setMatchedAnswers(List<String> matchedAnswers) {
        this.matchedAnswers = matchedAnswers;
    }
    
    public List<String> getMatchedChapters() {
        return matchedChapters;
    }
    
    public void setMatchedChapters(List<String> matchedChapters) {
        this.matchedChapters = matchedChapters;
    }
}


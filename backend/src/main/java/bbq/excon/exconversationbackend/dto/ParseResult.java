package bbq.excon.exconversationbackend.dto;

import java.util.List;

public class ParseResult {
    private boolean success;
    private String message;
    private int totalQuestions;
    private int totalAnswers;
    private int totalChapters;
    private int totalImages;
    private List<String> errors;
    private List<String> warnings;
    
    // Constructors
    public ParseResult() {
        this.errors = new java.util.ArrayList<>();
        this.warnings = new java.util.ArrayList<>();
    }
    
    public ParseResult(boolean success, String message) {
        this();
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
    
    public int getTotalQuestions() {
        return totalQuestions;
    }
    
    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }
    
    public int getTotalAnswers() {
        return totalAnswers;
    }
    
    public void setTotalAnswers(int totalAnswers) {
        this.totalAnswers = totalAnswers;
    }
    
    public int getTotalChapters() {
        return totalChapters;
    }
    
    public void setTotalChapters(int totalChapters) {
        this.totalChapters = totalChapters;
    }
    
    public int getTotalImages() {
        return totalImages;
    }
    
    public void setTotalImages(int totalImages) {
        this.totalImages = totalImages;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public void addError(String error) {
        this.errors.add(error);
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}


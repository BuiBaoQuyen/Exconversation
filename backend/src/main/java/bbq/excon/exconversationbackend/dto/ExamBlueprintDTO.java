package bbq.excon.exconversationbackend.dto;

import java.util.List;

public class ExamBlueprintDTO {
    private Long id;
    private String name;
    private Integer totalQuestions;
    private String description;
    private List<BlueprintDetailDTO> details;
    
    // Constructors
    public ExamBlueprintDTO() {
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
    
    public List<BlueprintDetailDTO> getDetails() {
        return details;
    }
    
    public void setDetails(List<BlueprintDetailDTO> details) {
        this.details = details;
    }
    
    public static class BlueprintDetailDTO {
        private Long id;
        private Long chapterId;
        private String chapterName;
        private Double percentage;
        
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
        
        public Double getPercentage() {
            return percentage;
        }
        
        public void setPercentage(Double percentage) {
            this.percentage = percentage;
        }
    }
}


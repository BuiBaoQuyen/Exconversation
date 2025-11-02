package bbq.excon.exconversationbackend.service;

import bbq.excon.exconversationbackend.dto.PatternTestResult;
import bbq.excon.exconversationbackend.dto.QuestionPatternDTO;
import bbq.excon.exconversationbackend.entity.QuestionPattern;
import bbq.excon.exconversationbackend.repository.QuestionPatternRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PatternService {
    
    @Autowired
    private QuestionPatternRepository patternRepository;
    
    public List<QuestionPatternDTO> getAllPatterns() {
        return patternRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public List<QuestionPatternDTO> getActivePatterns() {
        return patternRepository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public Optional<QuestionPatternDTO> getPatternById(Long id) {
        return patternRepository.findById(id)
                .map(this::convertToDTO);
    }
    
    @Transactional
    public QuestionPatternDTO createPattern(QuestionPatternDTO dto) {
        QuestionPattern pattern = new QuestionPattern();
        pattern.setPatternName(dto.getPatternName());
        pattern.setQuestionPattern(dto.getQuestionPattern());
        pattern.setAnswerPattern(dto.getAnswerPattern());
        pattern.setChapterDetector(dto.getChapterDetector());
        pattern.setExampleText(dto.getExampleText());
        pattern.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        pattern.setCreatedByName("System");
        pattern.setCreatedAt(LocalDateTime.now());
        pattern.setUpdatedAt(LocalDateTime.now());
        
        QuestionPattern saved = patternRepository.save(pattern);
        return convertToDTO(saved);
    }
    
    @Transactional
    public Optional<QuestionPatternDTO> updatePattern(Long id, QuestionPatternDTO dto) {
        return patternRepository.findById(id).map(pattern -> {
            pattern.setPatternName(dto.getPatternName());
            pattern.setQuestionPattern(dto.getQuestionPattern());
            pattern.setAnswerPattern(dto.getAnswerPattern());
            pattern.setChapterDetector(dto.getChapterDetector());
            pattern.setExampleText(dto.getExampleText());
            if (dto.getIsActive() != null) {
                pattern.setIsActive(dto.getIsActive());
            }
            pattern.setUpdatedAt(LocalDateTime.now());
            
            QuestionPattern updated = patternRepository.save(pattern);
            return convertToDTO(updated);
        });
    }
    
    @Transactional
    public boolean deletePattern(Long id) {
        if (patternRepository.existsById(id)) {
            patternRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public PatternTestResult testPattern(Long patternId, String testText) {
        Optional<QuestionPattern> patternOpt = patternRepository.findById(patternId);
        
        if (!patternOpt.isPresent()) {
            return new PatternTestResult(false, "Pattern not found");
        }
        
        QuestionPattern pattern = patternOpt.get();
        PatternTestResult result = new PatternTestResult();
        List<String> matchedQuestions = new ArrayList<>();
        List<String> matchedAnswers = new ArrayList<>();
        List<String> matchedChapters = new ArrayList<>();
        
        try {
            // Test question pattern
            if (pattern.getQuestionPattern() != null && !pattern.getQuestionPattern().isEmpty()) {
                Pattern questionRegex = Pattern.compile(pattern.getQuestionPattern(), Pattern.MULTILINE);
                Matcher matcher = questionRegex.matcher(testText);
                while (matcher.find()) {
                    matchedQuestions.add(matcher.group());
                }
            }
            
            // Test answer pattern
            if (pattern.getAnswerPattern() != null && !pattern.getAnswerPattern().isEmpty()) {
                Pattern answerRegex = Pattern.compile(pattern.getAnswerPattern(), Pattern.MULTILINE);
                Matcher matcher = answerRegex.matcher(testText);
                while (matcher.find()) {
                    matchedAnswers.add(matcher.group());
                }
            }
            
            // Test chapter detector
            if (pattern.getChapterDetector() != null && !pattern.getChapterDetector().isEmpty()) {
                Pattern chapterRegex = Pattern.compile(pattern.getChapterDetector(), Pattern.CASE_INSENSITIVE);
                Matcher matcher = chapterRegex.matcher(testText);
                while (matcher.find()) {
                    matchedChapters.add(matcher.group());
                }
            }
            
            result.setSuccess(true);
            result.setMessage(String.format("Found %d questions, %d answers, %d chapters", 
                    matchedQuestions.size(), matchedAnswers.size(), matchedChapters.size()));
            result.setMatchedQuestions(matchedQuestions);
            result.setMatchedAnswers(matchedAnswers);
            result.setMatchedChapters(matchedChapters);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error testing pattern: " + e.getMessage());
        }
        
        return result;
    }
    
    private QuestionPatternDTO convertToDTO(QuestionPattern pattern) {
        QuestionPatternDTO dto = new QuestionPatternDTO();
        dto.setId(pattern.getId());
        dto.setPatternName(pattern.getPatternName());
        dto.setQuestionPattern(pattern.getQuestionPattern());
        dto.setAnswerPattern(pattern.getAnswerPattern());
        dto.setChapterDetector(pattern.getChapterDetector());
        dto.setExampleText(pattern.getExampleText());
        dto.setIsActive(pattern.getIsActive());
        return dto;
    }
}


package bbq.excon.exconversationbackend.service;

import bbq.excon.exconversationbackend.dto.AnswerDTO;
import bbq.excon.exconversationbackend.dto.ImageDTO;
import bbq.excon.exconversationbackend.dto.QuestionDTO;
import bbq.excon.exconversationbackend.entity.*;
import bbq.excon.exconversationbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuestionService {
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private QuestionVersionRepository questionVersionRepository;
    
    @Autowired
    private AnswerRepository answerRepository;
    
    @Autowired
    private ImageRepository imageRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    /**
     * Get all questions
     */
    public List<QuestionDTO> getAllQuestions() {
        return questionRepository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get questions by chapter
     */
    public List<QuestionDTO> getQuestionsByChapter(Long chapterId) {
        return questionRepository.findByChapterIdAndIsActiveTrue(chapterId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get question by ID
     */
    public Optional<QuestionDTO> getQuestionById(Long id) {
        return questionRepository.findById(id)
                .map(this::convertToDTO);
    }
    
    /**
     * Update question
     */
    @Transactional
    public Optional<QuestionDTO> updateQuestion(Long id, QuestionDTO dto) {
        return questionRepository.findById(id).map(question -> {
            // Update chapter if changed
            if (dto.getChapterId() != null && 
                (question.getChapter() == null || !question.getChapter().getId().equals(dto.getChapterId()))) {
                Chapter chapter = chapterRepository.findById(dto.getChapterId())
                        .orElse(null);
                question.setChapter(chapter);
            }
            
            // Update type if changed
            if (dto.getType() != null) {
                question.setType(dto.getType());
            }
            
            // Update isActive if changed
            if (dto.getIsActive() != null) {
                question.setIsActive(dto.getIsActive());
            }
            
            questionRepository.save(question);
            
            // Update current version if exists
            if (dto.getCurrentVersionId() != null) {
                Optional<QuestionVersion> versionOpt = questionVersionRepository.findById(dto.getCurrentVersionId());
                versionOpt.ifPresent(version -> {
                    if (dto.getTitle() != null) {
                        version.setTitle(dto.getTitle());
                    }
                    if (dto.getContent() != null) {
                        version.setContent(dto.getContent());
                    }
                    questionVersionRepository.save(version);
                    
                    // Update answers
                    if (dto.getAnswers() != null) {
                        updateAnswers(version, dto.getAnswers());
                    }
                });
            }
            
            return convertToDTO(question);
        });
    }
    
    /**
     * Update answers
     */
    private void updateAnswers(QuestionVersion version, List<AnswerDTO> answerDTOs) {
        // Get existing answers
        List<Answer> existingAnswers = answerRepository.findByQuestionVersionId(version.getId());
        
        // Delete existing answers
        answerRepository.deleteAll(existingAnswers);
        
        // Create new answers
        for (AnswerDTO answerDTO : answerDTOs) {
            Answer answer = new Answer();
            answer.setQuestionVersion(version);
            answer.setOrderLabel(answerDTO.getOrderLabel());
            answer.setContent(answerDTO.getContent());
            answer.setIsCorrect(answerDTO.getIsCorrect() != null ? answerDTO.getIsCorrect() : false);
            answerRepository.save(answer);
        }
    }
    
    /**
     * Create new question version
     */
    @Transactional
    public Optional<QuestionVersion> createQuestionVersion(Long questionId, QuestionDTO dto) {
        return questionRepository.findById(questionId).map(question -> {
            // Get latest version number
            List<QuestionVersion> versions = questionVersionRepository.findByQuestionId(questionId);
            int newVersionNumber = versions.size() + 1;
            
            // Create new version
            QuestionVersion version = new QuestionVersion();
            version.setQuestion(question);
            version.setVersionNumber(newVersionNumber);
            version.setTitle(dto.getTitle());
            version.setContent(dto.getContent());
            version.setCreatedByName("System");
            version.setIsPublished(false);
            version = questionVersionRepository.save(version);
            
            // Create answers
            if (dto.getAnswers() != null) {
                for (AnswerDTO answerDTO : dto.getAnswers()) {
                    Answer answer = new Answer();
                    answer.setQuestionVersion(version);
                    answer.setOrderLabel(answerDTO.getOrderLabel());
                    answer.setContent(answerDTO.getContent());
                    answer.setIsCorrect(answerDTO.getIsCorrect() != null ? answerDTO.getIsCorrect() : false);
                    answerRepository.save(answer);
                }
            }
            
            return version;
        });
    }
    
    /**
     * Delete question (soft delete)
     */
    @Transactional
    public boolean deleteQuestion(Long id) {
        Optional<Question> questionOpt = questionRepository.findById(id);
        if (questionOpt.isPresent()) {
            Question question = questionOpt.get();
            question.setIsActive(false);
            questionRepository.save(question);
            return true;
        }
        return false;
    }
    
    /**
     * Convert Question entity to DTO
     */
    private QuestionDTO convertToDTO(Question question) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setType(question.getType());
        dto.setIsActive(question.getIsActive());
        
        if (question.getChapter() != null) {
            dto.setChapterId(question.getChapter().getId());
            dto.setChapterName(question.getChapter().getChapterName());
        }
        
        // Get published version or latest version
        Optional<QuestionVersion> versionOpt = questionVersionRepository
                .findByQuestionIdAndIsPublishedTrue(question.getId());
        
        if (!versionOpt.isPresent()) {
            List<QuestionVersion> versions = questionVersionRepository.findByQuestionId(question.getId());
            if (!versions.isEmpty()) {
                versionOpt = Optional.of(versions.get(versions.size() - 1));
            }
        }
        
        if (versionOpt.isPresent()) {
            QuestionVersion version = versionOpt.get();
            dto.setCurrentVersionId(version.getId());
            dto.setTitle(version.getTitle());
            dto.setContent(version.getContent());
            
            // Get answers
            List<Answer> answers = answerRepository.findByQuestionVersionIdOrderByOrderLabel(version.getId());
            dto.setAnswers(answers.stream().map(this::convertAnswerToDTO).collect(Collectors.toList()));
            
            // Get images
            List<Image> images = imageRepository.findByQuestionVersionId(version.getId());
            dto.setImages(images.stream().map(this::convertImageToDTO).collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    private AnswerDTO convertAnswerToDTO(Answer answer) {
        AnswerDTO dto = new AnswerDTO();
        dto.setId(answer.getId());
        dto.setOrderLabel(answer.getOrderLabel());
        dto.setContent(answer.getContent());
        dto.setIsCorrect(answer.getIsCorrect());
        return dto;
    }
    
    private ImageDTO convertImageToDTO(Image image) {
        ImageDTO dto = new ImageDTO();
        dto.setId(image.getId());
        dto.setImagePath(image.getImagePath());
        dto.setDescription(image.getDescription());
        return dto;
    }
}


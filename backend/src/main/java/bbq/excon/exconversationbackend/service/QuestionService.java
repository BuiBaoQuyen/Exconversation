package bbq.excon.exconversationbackend.service;

import bbq.excon.exconversationbackend.dto.AnswerDTO;
import bbq.excon.exconversationbackend.dto.ImageDTO;
import bbq.excon.exconversationbackend.dto.QuestionDTO;
import bbq.excon.exconversationbackend.entity.*;
import bbq.excon.exconversationbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
    
    @Autowired
    private OMMLToMathMLConverter ommlToMathMLConverter;
    
    /**
     * Get all questions (without content to save memory)
     * @deprecated Use getAllQuestionsPaginated instead for better performance
     */
    @Deprecated
    public List<QuestionDTO> getAllQuestions() {
        return questionRepository.findByIsActiveTrue().stream()
                .map(this::convertToDTOWithoutContent)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all questions with pagination (with full content for display)
     */
    public Page<QuestionDTO> getAllQuestionsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Question> questionPage = questionRepository.findByIsActiveTrue(pageable);
        return questionPage.map(this::convertToDTO);
    }
    
    /**
     * Get questions by chapter (without content to save memory)
     * @deprecated Use getQuestionsByChapterPaginated instead for better performance
     */
    @Deprecated
    public List<QuestionDTO> getQuestionsByChapter(Long chapterId) {
        return questionRepository.findByChapterIdAndIsActiveTrue(chapterId).stream()
                .map(this::convertToDTOWithoutContent)
                .collect(Collectors.toList());
    }
    
    /**
     * Get questions by chapter with pagination (with full content for display)
     */
    public Page<QuestionDTO> getQuestionsByChapterPaginated(Long chapterId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Question> questionPage = questionRepository.findByChapterIdAndIsActiveTrue(chapterId, pageable);
        return questionPage.map(this::convertToDTO);
    }
    
    /**
     * Get question by ID
     */
    public Optional<QuestionDTO> getQuestionById(Long id) {
        try {
            System.out.println("Fetching question by ID: " + id);
            Optional<Question> questionOpt = questionRepository.findById(id);
            
            if (!questionOpt.isPresent()) {
                System.out.println("Question not found: " + id);
                return Optional.empty();
            }
            
            Question question = questionOpt.get();
            System.out.println("Question found: " + question.getId() + ", Chapter: " + 
                             (question.getChapter() != null ? question.getChapter().getChapterName() : "null"));
            
            QuestionDTO dto = convertToDTO(question);
            
            System.out.println("QuestionDTO created - ContentLatex length: " + 
                             (dto.getContentLatex() != null ? dto.getContentLatex().length() : 0) + 
                             ", Answers count: " + (dto.getAnswers() != null ? dto.getAnswers().size() : 0));
            
            return Optional.of(dto);
        } catch (Exception e) {
            System.err.println("ERROR fetching question by ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
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
            
            // Update chapter name if provided (create or update chapter)
            if (dto.getChapterName() != null && !dto.getChapterName().trim().isEmpty()) {
                // Try to find existing chapter by name
                Chapter existingChapter = chapterRepository.findByChapterName(dto.getChapterName())
                        .orElse(null);
                
                if (existingChapter == null) {
                    // Create new chapter if not exists
                    existingChapter = new Chapter();
                    existingChapter.setChapterName(dto.getChapterName());
                    // Try to extract chapter index from name
                    try {
                        String chapterIndexStr = dto.getChapterName().replaceAll("\\D", "");
                        if (!chapterIndexStr.isEmpty()) {
                            existingChapter.setChapterIndex(Integer.parseInt(chapterIndexStr));
                        } else {
                            existingChapter.setChapterIndex(1);
                        }
                    } catch (Exception e) {
                        existingChapter.setChapterIndex(1);
                    }
                    existingChapter = chapterRepository.save(existingChapter);
                }
                
                question.setChapter(existingChapter);
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
                    // Prefer contentLatex/contentOmml from DTO
                    if (dto.getContentOmml() != null) {
                        version.setContentOmml(dto.getContentOmml());
                    }
                    if (dto.getContentLatex() != null) {
                        version.setContentLatex(dto.getContentLatex());
                    }
                    // Auto-convert nếu thiếu LaTeX nhưng có OMML
                    if ((version.getContentLatex() == null || version.getContentLatex().trim().isEmpty())
                            && version.getContentOmml() != null && !version.getContentOmml().trim().isEmpty()) {
                        String latex = ommlToMathMLConverter.convertContentOMMLToMathML(version.getContentOmml());
                        version.setContentLatex((latex != null && !latex.trim().isEmpty()) ? latex : null);
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
            answer.setContentOmml(answerDTO.getContentOmml());
            answer.setContentLatex(answerDTO.getContentLatex());
            if ((answer.getContentLatex() == null || answer.getContentLatex().trim().isEmpty())
                    && answer.getContentOmml() != null && !answer.getContentOmml().trim().isEmpty()) {
                String latex = ommlToMathMLConverter.convertContentOMMLToMathML(answer.getContentOmml());
                answer.setContentLatex((latex != null && !latex.trim().isEmpty()) ? latex : null);
            }
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
            version.setContentOmml(dto.getContentOmml());
            version.setContentLatex(dto.getContentLatex());
            if ((version.getContentLatex() == null || version.getContentLatex().trim().isEmpty())
                    && version.getContentOmml() != null && !version.getContentOmml().trim().isEmpty()) {
                String latex = ommlToMathMLConverter.convertContentOMMLToMathML(version.getContentOmml());
                version.setContentLatex((latex != null && !latex.trim().isEmpty()) ? latex : null);
            }
            version.setCreatedByName("System");
            version.setIsPublished(false);
            version = questionVersionRepository.save(version);
            
            // Create answers
            if (dto.getAnswers() != null) {
                for (AnswerDTO answerDTO : dto.getAnswers()) {
                    Answer answer = new Answer();
                    answer.setQuestionVersion(version);
                    answer.setOrderLabel(answerDTO.getOrderLabel());
                    answer.setContentOmml(answerDTO.getContentOmml());
                    answer.setContentLatex(answerDTO.getContentLatex());
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
     * Convert Question entity to DTO (with full content - for detail view)
     */
    private QuestionDTO convertToDTO(Question question) {
        try {
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
                dto.setTitle(version.getTitle() != null ? version.getTitle() : "");
                
                // Ưu tiên contentLatex -> FE render trực tiếp
                String contentLatex = version.getContentLatex();
                if (contentLatex == null || contentLatex.trim().isEmpty()) {
                    // Fallback: convert từ OMML nếu chưa có
                    String omml = version.getContentOmml();
                    if (omml != null && !omml.trim().isEmpty()) {
                        contentLatex = ommlToMathMLConverter.convertContentOMMLToMathML(omml);
                    }
                }
                dto.setContentLatex(contentLatex != null ? contentLatex : "");

                // Trả kèm OMML để FE có thể dùng khi cần (optional)
                dto.setContentOmml(version.getContentOmml());

                System.out.println("Loaded content for question " + question.getId() + 
                                 " - Length: " + (dto.getContentLatex() != null ? dto.getContentLatex().length() : 0));
                
                // Get answers with content
                try {
                    List<Answer> answers = answerRepository.findByQuestionVersionIdOrderByOrderLabel(version.getId());
                    dto.setAnswers(answers.stream()
                            .map(this::convertAnswerToDTO)
                            .collect(Collectors.toList()));
                    System.out.println("Loaded " + answers.size() + " answers for question " + question.getId());
                } catch (Exception e) {
                    System.err.println("ERROR loading answers for question " + question.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                    dto.setAnswers(new ArrayList<>());
                }
                
                // Get images
                try {
                    List<Image> images = imageRepository.findByQuestionVersionId(version.getId());
                    dto.setImages(images.stream()
                            .map(this::convertImageToDTO)
                            .collect(Collectors.toList()));
                } catch (Exception e) {
                    System.err.println("ERROR loading images for question " + question.getId() + ": " + e.getMessage());
                    dto.setImages(new ArrayList<>());
                }
            } else {
                System.out.println("WARNING: No version found for question " + question.getId());
                dto.setContentLatex("");
                dto.setContentOmml("");
                dto.setAnswers(new ArrayList<>());
                dto.setImages(new ArrayList<>());
            }
            
            return dto;
        } catch (Exception e) {
            System.err.println("ERROR converting Question to DTO: " + e.getMessage());
            e.printStackTrace();
            // Return minimal DTO to avoid breaking frontend
            QuestionDTO dto = new QuestionDTO();
            dto.setId(question.getId());
            dto.setType(question.getType());
            dto.setIsActive(question.getIsActive());
            dto.setContentLatex("");
            dto.setContentOmml("");
            dto.setAnswers(new ArrayList<>());
            dto.setImages(new ArrayList<>());
            return dto;
        }
    }
    
    /**
     * Convert Question entity to DTO without content (for list view - saves memory)
     */
    private QuestionDTO convertToDTOWithoutContent(Question question) {
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
            // DO NOT load content to save memory
            dto.setContentLatex(null);
            dto.setContentOmml(null);
            
            // DO NOT load answers and images to save memory
            dto.setAnswers(null);
            dto.setImages(null);
        }
        
        return dto;
    }
    
    private AnswerDTO convertAnswerToDTO(Answer answer) {
        AnswerDTO dto = new AnswerDTO();
        dto.setId(answer.getId());
        dto.setOrderLabel(answer.getOrderLabel());

        // Ưu tiên LaTeX
        String latex = answer.getContentLatex();
        if (latex == null || latex.trim().isEmpty()) {
            String omml = answer.getContentOmml();
            if (omml != null && !omml.trim().isEmpty()) {
                latex = ommlToMathMLConverter.convertContentOMMLToMathML(omml);
            }
        }
        dto.setContentLatex(latex);
        dto.setContentOmml(answer.getContentOmml());

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


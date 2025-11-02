package bbq.excon.exconversationbackend.service;

import bbq.excon.exconversationbackend.dto.ExamDTO;
import bbq.excon.exconversationbackend.dto.QuestionDTO;
import bbq.excon.exconversationbackend.entity.*;
import bbq.excon.exconversationbackend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExamService {
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private ExamQuestionRepository examQuestionRepository;
    
    @Autowired
    private ExamBlueprintRepository blueprintRepository;
    
    @Autowired
    private ExamBlueprintDetailRepository blueprintDetailRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private QuestionVersionRepository questionVersionRepository;
    
    @Autowired
    private ExamBlueprintService blueprintService;
    
    private final SecureRandom random = new SecureRandom();
    
    /**
     * Generate exam from blueprint
     */
    @Transactional
    public ExamDTO generateExam(Long blueprintId, String examName, String createdByName) {
        ExamBlueprint blueprint = blueprintRepository.findById(blueprintId)
                .orElseThrow(() -> new RuntimeException("Blueprint not found: " + blueprintId));
        
        // Calculate questions per chapter
        Map<Long, Integer> questionsPerChapter = blueprintService.calculateQuestionsPerChapter(blueprint);
        
        // Create exam
        Exam exam = new Exam();
        exam.setBlueprint(blueprint);
        exam.setName(examName != null ? examName : "Exam from " + blueprint.getName());
        exam.setCreatedAt(LocalDateTime.now());
        exam.setNote("Generated from blueprint: " + blueprint.getName());
        exam = examRepository.save(exam);
        
        // Generate questions
        List<ExamQuestion> examQuestions = new ArrayList<>();
        int orderNumber = 1;
        
        for (Map.Entry<Long, Integer> entry : questionsPerChapter.entrySet()) {
            Long chapterId = entry.getKey();
            int questionsNeeded = entry.getValue();
            
            // Get available questions from chapter
            List<Question> availableQuestions = questionRepository
                    .findActiveQuestionsByChapter(chapterId);
            
            if (availableQuestions.size() < questionsNeeded) {
                throw new RuntimeException("Not enough questions in chapter " + chapterId + 
                        ". Available: " + availableQuestions.size() + ", Needed: " + questionsNeeded);
            }
            
            // Random select questions
            List<Question> selectedQuestions = randomSelect(availableQuestions, questionsNeeded);
            
            // Add to exam
            for (Question question : selectedQuestions) {
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
                    ExamQuestion examQuestion = new ExamQuestion();
                    examQuestion.setExam(exam);
                    examQuestion.setQuestionVersion(versionOpt.get());
                    examQuestion.setOrderNumber(orderNumber++);
                    examQuestions.add(examQuestion);
                }
            }
        }
        
        // Randomize order (optional)
        Collections.shuffle(examQuestions, random);
        for (int i = 0; i < examQuestions.size(); i++) {
            examQuestions.get(i).setOrderNumber(i + 1);
        }
        
        // Save exam questions
        examQuestionRepository.saveAll(examQuestions);
        
        // Convert to DTO
        return convertToDTO(exam);
    }
    
    /**
     * Random select questions without replacement
     */
    private List<Question> randomSelect(List<Question> questions, int count) {
        List<Question> result = new ArrayList<>();
        List<Question> available = new ArrayList<>(questions);
        
        for (int i = 0; i < count && !available.isEmpty(); i++) {
            int index = random.nextInt(available.size());
            result.add(available.remove(index));
        }
        
        return result;
    }
    
    /**
     * Get exam by ID
     */
    public Optional<ExamDTO> getExamById(Long id) {
        return examRepository.findById(id)
                .map(this::convertToDTO);
    }
    
    /**
     * Get all exams
     */
    public List<ExamDTO> getAllExams() {
        return examRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Delete exam
     */
    @Transactional
    public boolean deleteExam(Long id) {
        if (examRepository.existsById(id)) {
            examRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    /**
     * Convert to DTO
     */
    private ExamDTO convertToDTO(Exam exam) {
        ExamDTO dto = new ExamDTO();
        dto.setId(exam.getId());
        dto.setName(exam.getName());
        dto.setNote(exam.getNote());
        
        if (exam.getBlueprint() != null) {
            dto.setBlueprintId(exam.getBlueprint().getId());
            dto.setBlueprintName(exam.getBlueprint().getName());
        }
        
        // Get exam questions
        List<ExamQuestion> examQuestions = examQuestionRepository
                .findByExamIdOrderByOrderNumber(exam.getId());
        
        List<ExamDTO.ExamQuestionDTO> questionDTOs = new ArrayList<>();
        for (ExamQuestion eq : examQuestions) {
            ExamDTO.ExamQuestionDTO eqDTO = new ExamDTO.ExamQuestionDTO();
            eqDTO.setId(eq.getId());
            eqDTO.setOrderNumber(eq.getOrderNumber());
            
            // Get question DTO
            Question question = eq.getQuestionVersion().getQuestion();
            QuestionDTO questionDTO = convertQuestionToDTO(question, eq.getQuestionVersion());
            eqDTO.setQuestion(questionDTO);
            
            questionDTOs.add(eqDTO);
        }
        
        dto.setQuestions(questionDTOs);
        return dto;
    }
    
    private QuestionDTO convertQuestionToDTO(Question question, QuestionVersion version) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setType(question.getType());
        dto.setIsActive(question.getIsActive());
        dto.setCurrentVersionId(version.getId());
        dto.setTitle(version.getTitle());
        dto.setContent(version.getContent());
        
        if (question.getChapter() != null) {
            dto.setChapterId(question.getChapter().getId());
            dto.setChapterName(question.getChapter().getChapterName());
        }
        
        return dto;
    }
}


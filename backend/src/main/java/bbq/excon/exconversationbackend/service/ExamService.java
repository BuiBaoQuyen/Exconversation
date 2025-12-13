package bbq.excon.exconversationbackend.service;

import bbq.excon.exconversationbackend.dto.ExamDTO;
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

@Service
public class ExamService {
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private ExamQuestionRepository examQuestionRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private QuestionVersionRepository questionVersionRepository;
    
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
                .collect(Collectors.toList());
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
        // Prefer MathML for display; keep OMML if needed by caller
        dto.setContentMathml(version.getContentMathml());
        dto.setContentOmml(version.getContentOmml());
        
        if (question.getChapter() != null) {
            dto.setChapterId(question.getChapter().getId());
            dto.setChapterName(question.getChapter().getChapterName());
        }
        
        return dto;
    }
}

package bbq.excon.exconversationbackend.service;

import bbq.excon.exconversationbackend.dto.ExamBlueprintDTO;
import bbq.excon.exconversationbackend.entity.Chapter;
import bbq.excon.exconversationbackend.entity.ExamBlueprint;
import bbq.excon.exconversationbackend.entity.ExamBlueprintDetail;
import bbq.excon.exconversationbackend.repository.ChapterRepository;
import bbq.excon.exconversationbackend.repository.ExamBlueprintDetailRepository;
import bbq.excon.exconversationbackend.repository.ExamBlueprintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExamBlueprintService {
    
    @Autowired
    private ExamBlueprintRepository blueprintRepository;
    
    @Autowired
    private ExamBlueprintDetailRepository detailRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    /**
     * Get all blueprints
     */
    public List<ExamBlueprintDTO> getAllBlueprints() {
        return blueprintRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get blueprint by ID
     */
    public Optional<ExamBlueprintDTO> getBlueprintById(Long id) {
        return blueprintRepository.findById(id)
                .map(this::convertToDTO);
    }
    
    /**
     * Create blueprint
     */
    @Transactional
    public ExamBlueprintDTO createBlueprint(ExamBlueprintDTO dto) {
        // Validate percentages
        if (dto.getDetails() != null) {
            double totalPercentage = dto.getDetails().stream()
                    .mapToDouble(ExamBlueprintDTO.BlueprintDetailDTO::getPercentage)
                    .sum();
            
            if (Math.abs(totalPercentage - 100.0) > 0.01) {
                throw new IllegalArgumentException("Total percentage must equal 100%, current: " + totalPercentage + "%");
            }
        }
        
        // Create blueprint
        ExamBlueprint blueprint = new ExamBlueprint();
        blueprint.setName(dto.getName());
        blueprint.setTotalQuestions(dto.getTotalQuestions());
        blueprint.setDescription(dto.getDescription());
        blueprint.setCreatedAt(LocalDateTime.now());
        blueprint = blueprintRepository.save(blueprint);
        
        // Create details
        if (dto.getDetails() != null) {
            for (ExamBlueprintDTO.BlueprintDetailDTO detailDTO : dto.getDetails()) {
                Chapter chapter = chapterRepository.findById(detailDTO.getChapterId())
                        .orElseThrow(() -> new RuntimeException("Chapter not found: " + detailDTO.getChapterId()));
                
                ExamBlueprintDetail detail = new ExamBlueprintDetail();
                detail.setExamBlueprint(blueprint);
                detail.setChapter(chapter);
                detail.setPercentage(detailDTO.getPercentage());
                detailRepository.save(detail);
            }
        }
        
        return convertToDTO(blueprint);
    }
    
    /**
     * Update blueprint
     */
    @Transactional
    public Optional<ExamBlueprintDTO> updateBlueprint(Long id, ExamBlueprintDTO dto) {
        return blueprintRepository.findById(id).map(blueprint -> {
            // Validate percentages
            if (dto.getDetails() != null) {
                double totalPercentage = dto.getDetails().stream()
                        .mapToDouble(ExamBlueprintDTO.BlueprintDetailDTO::getPercentage)
                        .sum();
                
                if (Math.abs(totalPercentage - 100.0) > 0.01) {
                    throw new IllegalArgumentException("Total percentage must equal 100%, current: " + totalPercentage + "%");
                }
            }
            
            // Update blueprint
            if (dto.getName() != null) {
                blueprint.setName(dto.getName());
            }
            if (dto.getTotalQuestions() != null) {
                blueprint.setTotalQuestions(dto.getTotalQuestions());
            }
            if (dto.getDescription() != null) {
                blueprint.setDescription(dto.getDescription());
            }
            blueprintRepository.save(blueprint);
            
            // Delete existing details
            detailRepository.deleteByExamBlueprintId(id);
            
            // Create new details
            if (dto.getDetails() != null) {
                for (ExamBlueprintDTO.BlueprintDetailDTO detailDTO : dto.getDetails()) {
                    Chapter chapter = chapterRepository.findById(detailDTO.getChapterId())
                            .orElseThrow(() -> new RuntimeException("Chapter not found: " + detailDTO.getChapterId()));
                    
                    ExamBlueprintDetail detail = new ExamBlueprintDetail();
                    detail.setExamBlueprint(blueprint);
                    detail.setChapter(chapter);
                    detail.setPercentage(detailDTO.getPercentage());
                    detailRepository.save(detail);
                }
            }
            
            return convertToDTO(blueprint);
        });
    }
    
    /**
     * Delete blueprint
     */
    @Transactional
    public boolean deleteBlueprint(Long id) {
        if (blueprintRepository.existsById(id)) {
            blueprintRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    /**
     * Calculate number of questions per chapter
     */
    public java.util.Map<Long, Integer> calculateQuestionsPerChapter(ExamBlueprint blueprint) {
        java.util.Map<Long, Integer> result = new java.util.HashMap<>();
        
        List<ExamBlueprintDetail> details = detailRepository.findByExamBlueprintId(blueprint.getId());
        int totalQuestions = blueprint.getTotalQuestions();
        int remainingQuestions = totalQuestions;
        int processedChapters = 0;
        
        // Calculate for each chapter
        for (ExamBlueprintDetail detail : details) {
            double percentage = detail.getPercentage();
            double calculatedQuestions = (totalQuestions * percentage) / 100.0;
            int questionsForChapter = (int) Math.round(calculatedQuestions);
            
            result.put(detail.getChapter().getId(), questionsForChapter);
            remainingQuestions -= questionsForChapter;
            processedChapters++;
        }
        
        // Adjust for rounding errors
        if (remainingQuestions != 0 && processedChapters > 0) {
            // Distribute remaining questions
            int perChapter = remainingQuestions / processedChapters;
            int extra = remainingQuestions % processedChapters;
            
            int index = 0;
            for (ExamBlueprintDetail detail : details) {
                int current = result.get(detail.getChapter().getId());
                result.put(detail.getChapter().getId(), current + perChapter + (index < extra ? 1 : 0));
                index++;
            }
        }
        
        return result;
    }
    
    /**
     * Convert to DTO
     */
    private ExamBlueprintDTO convertToDTO(ExamBlueprint blueprint) {
        ExamBlueprintDTO dto = new ExamBlueprintDTO();
        dto.setId(blueprint.getId());
        dto.setName(blueprint.getName());
        dto.setTotalQuestions(blueprint.getTotalQuestions());
        dto.setDescription(blueprint.getDescription());
        
        // Get details
        List<ExamBlueprintDetail> details = detailRepository.findByExamBlueprintId(blueprint.getId());
        dto.setDetails(details.stream().map(detail -> {
            ExamBlueprintDTO.BlueprintDetailDTO detailDTO = new ExamBlueprintDTO.BlueprintDetailDTO();
            detailDTO.setId(detail.getId());
            detailDTO.setChapterId(detail.getChapter().getId());
            detailDTO.setChapterName(detail.getChapter().getChapterName());
            detailDTO.setPercentage(detail.getPercentage());
            return detailDTO;
        }).collect(Collectors.toList()));
        
        return dto;
    }
}


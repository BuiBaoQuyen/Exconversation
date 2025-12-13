package bbq.excon.exconversationbackend.parser;

import bbq.excon.exconversationbackend.entity.*;
import bbq.excon.exconversationbackend.repository.*;
import bbq.excon.exconversationbackend.service.MathMLConverterService;
import bbq.excon.exconversationbackend.service.OMMLToMathMLConverterService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service để parse Word document và extract questions với text + OMML
 * 
 * Parse DOCX file và extract:
 * - Text content từ paragraphs
 * - OMML (equations) từ paragraphs
 * - Combine text + OMML với format: "Text <omml>...</omml> Text"
 * - Lưu vào QuestionVersion.content
 */
@Service
public class DocumentParserService {
    
    @Autowired
    private UploadRepository uploadRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private QuestionVersionRepository questionVersionRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private MathMLConverterService mathMLConverterService;

    @Autowired
    private OMMLToMathMLConverterService ommlToMathMLConverterService;
    
    /**
     * Parse Word document và extract questions
     * Async processing để không block request
     * 
     * @param uploadId ID của Upload record
     */
    @Async("aiParseExecutor")
    @Transactional
    public void parseDocument(Long uploadId) {
        try {
            System.out.println("Starting document parsing for upload ID: " + uploadId);
            
            // Load Upload entity
            Upload upload = uploadRepository.findById(uploadId != null ? uploadId : 0L)
                    .orElseThrow(() -> new RuntimeException("Upload not found: " + uploadId));
            
            // Update status to parsing
            upload.setStatus("parsing");
            uploadRepository.save(upload);
            
            // Load Word document
            XWPFDocument document = null;
            try {
                document = new XWPFDocument(new FileInputStream(upload.getFilePath()));
                
                // Get or create default chapter
                Chapter chapter = getOrCreateDefaultChapter(upload.getUploadedByName());
                
                // Extract paragraphs and create questions
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                System.out.println("Found " + paragraphs.size() + " paragraphs");
                
                int questionNumber = 0;
                for (XWPFParagraph para : paragraphs) {
                    String paraText = para.getText();
                    if (paraText == null || paraText.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Check if paragraph looks like a question
                    // Pattern: "Câu 1.", "Câu 2.", "1.", "2.", etc.
                    if (isQuestionParagraph(paraText)) {
                        questionNumber++;
                        System.out.println("Processing question " + questionNumber + ": " + 
                                         paraText.substring(0, Math.min(50, paraText.length())));
                        
                        // Extract content (text + OMML)
                        String contentOmml = mathMLConverterService.extractContentWithMath(para);
                        
                        if (contentOmml != null && !contentOmml.trim().isEmpty()) {
                            // Convert sang MathML cho FE hiển thị
                            String contentMathml = ommlToMathMLConverterService.convertContentOMMLToMathML(contentOmml);
                            if (contentMathml != null && contentMathml.trim().isEmpty()) {
                                contentMathml = null; // tránh lưu MathML hỏng
                            }
                            
                            // Create Question
                            Question question = new Question();
                            question.setChapter(chapter);
                            question.setType("Trắc nghiệm");
                            question.setCreatedByName(upload.getUploadedByName());
                            question.setIsActive(true);
                            question = questionRepository.save(question);
                            
                            // Create QuestionVersion with content
                            QuestionVersion version = new QuestionVersion();
                            version.setQuestion(question);
                            version.setVersionNumber(1);
                            version.setTitle("Câu " + questionNumber);
                            version.setContentOmml(contentOmml); // Format: "Text <omml>...</omml> Text"
                            version.setContentMathml(contentMathml); // Format: "Text <math>...</math> Text"
                            version.setCreatedByName(upload.getUploadedByName());
                            version.setIsPublished(false);
                            questionVersionRepository.save(version);
                            
                            System.out.println("Created question " + questionNumber + 
                                             " with content length: " + contentOmml.length());
                        }
                    }
                }
                
                // Update status to parsed
                upload.setStatus("parsed");
                upload.setNote("Successfully parsed " + questionNumber + " questions");
                uploadRepository.save(upload);
                
                System.out.println("Document parsing completed. Created " + questionNumber + " questions");
                
            } finally {
                if (document != null) {
                    try {
                        document.close();
                    } catch (IOException e) {
                        System.err.println("Error closing document: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing document: " + e.getMessage());
            e.printStackTrace();
            
            // Update status to error
            try {
                if (uploadId != null) {
                    Upload upload = uploadRepository.findById(uploadId).orElse(null);
                    if (upload != null) {
                        upload.setStatus("error");
                        upload.setNote("Parsing failed: " + e.getMessage());
                        uploadRepository.save(upload);
                    }
                }
            } catch (Exception ex) {
                System.err.println("Error updating upload status: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Check if paragraph looks like a question
     * Patterns: "Câu 1.", "Câu 2.", "1.", "2.", "Question 1", etc.
     */
    private boolean isQuestionParagraph(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // Pattern: "Câu" followed by number
        Pattern cauPattern = Pattern.compile("^\\s*Câu\\s+\\d+", Pattern.CASE_INSENSITIVE);
        if (cauPattern.matcher(text).find()) {
            return true;
        }
        
        // Pattern: Number followed by dot at start
        Pattern numberPattern = Pattern.compile("^\\s*\\d+\\s*[.\\-)]");
        if (numberPattern.matcher(text).find()) {
            return true;
        }
        
        // Pattern: "Question" followed by number
        Pattern questionPattern = Pattern.compile("^\\s*Question\\s+\\d+", Pattern.CASE_INSENSITIVE);
        if (questionPattern.matcher(text).find()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get or create default chapter
     */
    private Chapter getOrCreateDefaultChapter(String createdByName) {
        // Try to find existing default chapter
        String defaultChapterName = "Chương mặc định";
        return chapterRepository.findByChapterName(defaultChapterName)
                .orElseGet(() -> {
                    // Create new default chapter
                    Chapter chapter = new Chapter();
                    chapter.setChapterName(defaultChapterName);
                    chapter.setChapterIndex(0);
                    chapter.setCreatedByName(createdByName);
                    return chapterRepository.save(chapter);
                });
    }
}


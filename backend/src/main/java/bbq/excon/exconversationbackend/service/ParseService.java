package bbq.excon.exconversationbackend.service;

import bbq.excon.exconversationbackend.dto.ParseResult;
import bbq.excon.exconversationbackend.entity.*;
import bbq.excon.exconversationbackend.repository.*;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ParseService {
    
    @Autowired
    private UploadRepository uploadRepository;
    
    @Autowired
    private QuestionPatternRepository patternRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private QuestionVersionRepository questionVersionRepository;
    
    @Autowired
    private AnswerRepository answerRepository;
    
    @Autowired
    private ImageRepository imageRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private MathMLConverterService mathMLConverterService;
    
    /**
     * Parse DOCX file với pattern đã chọn
     */
    @Transactional
    public ParseResult parseDocument(Long uploadId, Long patternId) {
        ParseResult result = new ParseResult();
        
        try {
            // Lấy upload record
            Upload upload = uploadRepository.findById(uploadId)
                    .orElseThrow(() -> new RuntimeException("Upload not found: " + uploadId));
            
            // Update status to parsing
            upload.setStatus("parsing");
            uploadRepository.save(upload);
            
            // Lấy pattern
            QuestionPattern questionPattern = patternRepository.findById(patternId)
                    .orElseThrow(() -> new RuntimeException("Pattern not found: " + patternId));
            
            // Đọc DOCX file
            XWPFDocument document = new XWPFDocument(new FileInputStream(upload.getFilePath()));
            
            // Parse document
            parseDocumentContent(document, questionPattern, upload.getUploadedByName(), result);
            
            // Update status
            upload.setStatus("parsed");
            upload.setNote("Parsed " + result.getTotalQuestions() + " questions");
            uploadRepository.save(upload);
            
            document.close();
            
            result.setSuccess(true);
            result.setMessage("Parsed successfully: " + result.getTotalQuestions() + " questions");
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error parsing document: " + e.getMessage());
            result.addError(e.getMessage());
            
            // Update upload status
            try {
                Upload upload = uploadRepository.findById(uploadId).orElse(null);
                if (upload != null) {
                    upload.setStatus("error");
                    upload.setNote("Error: " + e.getMessage());
                    uploadRepository.save(upload);
                }
            } catch (Exception ex) {
                // Ignore
            }
        }
        
        return result;
    }
    
    /**
     * Parse nội dung document
     */
    private void parseDocumentContent(XWPFDocument document, 
                                     QuestionPattern pattern, 
                                     String createdByName,
                                     ParseResult result) throws Exception {
        
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        List<XWPFPictureData> pictures = document.getAllPictures();
        
        // Compile patterns
        Pattern questionPattern = Pattern.compile(pattern.getQuestionPattern(), Pattern.MULTILINE);
        Pattern answerPattern = Pattern.compile(pattern.getAnswerPattern(), Pattern.MULTILINE);
        Pattern chapterPattern = pattern.getChapterDetector() != null && !pattern.getChapterDetector().isEmpty()
                ? Pattern.compile(pattern.getChapterDetector(), Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
                : null;
        
        // Current state
        Chapter currentChapter = null;
        Question currentQuestion = null;
        QuestionVersion currentQuestionVersion = null;
        StringBuilder currentQuestionText = new StringBuilder();
        List<Answer> currentAnswers = new ArrayList<>();
        int questionNumber = 0;
        int imageIndex = 0;
        
        for (XWPFParagraph para : paragraphs) {
            String text = para.getText().trim();
            
            if (text.isEmpty()) {
                continue;
            }
            
            // Check for chapter
            if (chapterPattern != null) {
                Matcher chapterMatcher = chapterPattern.matcher(text);
                if (chapterMatcher.find()) {
                    // Tìm hoặc tạo chapter
                    String chapterNumberStr = chapterMatcher.group(1);
                    try {
                        int chapterIndex = Integer.parseInt(chapterNumberStr);
                        currentChapter = chapterRepository.findByChapterIndex(chapterIndex)
                                .orElse(null);
                        
                        if (currentChapter == null) {
                            currentChapter = new Chapter();
                            currentChapter.setChapterIndex(chapterIndex);
                            currentChapter.setChapterName(text);
                            currentChapter.setCreatedByName(createdByName);
                            currentChapter = chapterRepository.save(currentChapter);
                            result.setTotalChapters(result.getTotalChapters() + 1);
                        }
                    } catch (NumberFormatException e) {
                        result.addWarning("Cannot parse chapter number: " + chapterNumberStr);
                    }
                }
            }
            
            // Check for question
            Matcher questionMatcher = questionPattern.matcher(text);
            if (questionMatcher.find()) {
                // Save previous question if exists
                if (currentQuestion != null) {
                    saveQuestion(currentQuestion, currentQuestionVersion, currentQuestionText.toString(),
                            currentAnswers, imageIndex, result);
                    currentAnswers.clear();
                    imageIndex++;
                }
                
                // Tạo question mới
                currentQuestion = new Question();
                currentQuestion.setChapter(currentChapter);
                currentQuestion.setType("Trắc nghiệm");
                currentQuestion.setCreatedByName(createdByName);
                currentQuestion = questionRepository.save(currentQuestion);
                
                // Tạo question version
                currentQuestionVersion = new QuestionVersion();
                currentQuestionVersion.setQuestion(currentQuestion);
                currentQuestionVersion.setVersionNumber(1);
                currentQuestionVersion.setTitle(text);
                currentQuestionVersion.setCreatedByName(createdByName);
                currentQuestionVersion.setIsPublished(true);
                currentQuestionVersion = questionVersionRepository.save(currentQuestionVersion);
                
                // Extract content từ paragraph (có thể chứa OMML)
                String contentWithMath = extractContentWithOMML(para);
                currentQuestionText = new StringBuilder();
                currentQuestionText.append(contentWithMath);
                
                questionNumber++;
                result.setTotalQuestions(result.getTotalQuestions() + 1);
            }
            
            // Check for answer
            Matcher answerMatcher = answerPattern.matcher(text);
            if (answerMatcher.find() && currentQuestionVersion != null) {
                String orderLabel = answerMatcher.group(1);
                String answerContent = answerMatcher.groupCount() > 1 ? answerMatcher.group(2) : "";
                
                // Extract content từ paragraph (có thể chứa OMML)
                String contentWithMath = extractContentWithOMML(para);
                
                Answer answer = new Answer();
                answer.setQuestionVersion(currentQuestionVersion);
                answer.setOrderLabel(orderLabel);
                answer.setContent(contentWithMath.isEmpty() ? answerContent : contentWithMath);
                // Mặc định đáp án đầu tiên là đúng (có thể thay đổi logic sau)
                answer.setIsCorrect(answerMatcher.group(1).equals("A") && currentAnswers.isEmpty());
                currentAnswers.add(answer);
                result.setTotalAnswers(result.getTotalAnswers() + 1);
            }
            
            // Append to current question text nếu không match pattern nào và đang ở trong question
            if (currentQuestionVersion != null) {
                // Re-check để tránh double matching
                boolean isQuestion = questionPattern.matcher(text).find();
                boolean isAnswer = answerPattern.matcher(text).find();
                
                if (!isQuestion && !isAnswer) {
                    if (currentQuestionText.length() > 0) {
                        currentQuestionText.append("\n");
                    }
                    currentQuestionText.append(extractContentWithOMML(para));
                }
            }
            
            // Extract images từ paragraphs (nếu có)
            extractImagesFromParagraph(para, currentQuestionVersion, result);
        }
        
        // Save last question
        if (currentQuestion != null) {
            saveQuestion(currentQuestion, currentQuestionVersion, currentQuestionText.toString(),
                    currentAnswers, imageIndex, result);
        }
    }
    
    /**
     * Extract content từ paragraph, bao gồm OMML equations
     * Convert OMML sang MathML để lưu vào database
     */
    private String extractContentWithOMML(XWPFParagraph para) {
        // Sử dụng MathMLConverterService để extract text và convert OMML
        return mathMLConverterService.extractContentWithMath(para);
    }
    
    /**
     * Extract images từ paragraph
     */
    private void extractImagesFromParagraph(XWPFParagraph para, QuestionVersion questionVersion, ParseResult result) {
        if (questionVersion == null) {
            return;
        }
        
        try {
            for (XWPFRun run : para.getRuns()) {
                for (XWPFPicture picture : run.getEmbeddedPictures()) {
                    try {
                        // Get image data
                        byte[] imageData = picture.getPictureData().getData();
                        String imageName = picture.getPictureData().getFileName();
                        
                        // Save image file
                        String imagePath = fileStorageService.storeImageFromBytes(
                            imageData,
                            imageName,
                            questionVersion.getQuestion().getId()
                        );
                        
                        // Save image record
                        Image image = new Image();
                        image.setQuestionVersion(questionVersion);
                        image.setImagePath(imagePath);
                        image.setDescription(imageName);
                        imageRepository.save(image);
                        
                        result.setTotalImages(result.getTotalImages() + 1);
                    } catch (Exception e) {
                        result.addWarning("Failed to extract image: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // Ignore nếu không có image trong paragraph này
        }
    }
    
    /**
     * Save question với answers
     */
    private void saveQuestion(Question question, QuestionVersion questionVersion, 
                             String content, List<Answer> answers, int imageIndex, 
                             ParseResult result) {
        // Update question version content
        questionVersion.setContent(content);
        questionVersionRepository.save(questionVersion);
        
        // Save answers
        for (Answer answer : answers) {
            answerRepository.save(answer);
        }
    }
}


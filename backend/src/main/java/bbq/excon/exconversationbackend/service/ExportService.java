package bbq.excon.exconversationbackend.service;

import bbq.excon.exconversationbackend.entity.*;
import bbq.excon.exconversationbackend.repository.AnswerRepository;
import bbq.excon.exconversationbackend.repository.ExamQuestionRepository;
import bbq.excon.exconversationbackend.repository.ExamRepository;
import bbq.excon.exconversationbackend.repository.ImageRepository;
import bbq.excon.exconversationbackend.repository.QuestionRepository;
import bbq.excon.exconversationbackend.repository.QuestionVersionRepository;
import bbq.excon.exconversationbackend.service.export.ContentParser;
import bbq.excon.exconversationbackend.service.export.ContentSegment;
import bbq.excon.exconversationbackend.service.export.OMMLInserter;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private ExamQuestionRepository examQuestionRepository;
    
    @Autowired
    private ImageRepository imageRepository;
    
    @Autowired
    private AnswerRepository answerRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private QuestionVersionRepository questionVersionRepository;
    
    @Value("${file.export-dir}")
    private String exportDir;
    
    /**
     * Export exam to DOCX file
     */
    public String exportExamToDOCX(Long examId, boolean includeAnswers) throws IOException {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found: " + examId));
        
        // Create export directory if not exists
        Path exportPath = Paths.get(exportDir);
        if (!Files.exists(exportPath)) {
            Files.createDirectories(exportPath);
        }
        
        // Create new DOCX document
        XWPFDocument document = new XWPFDocument();
        
        // Add title
        addTitle(document, exam.getName());
        
        // Get exam questions ordered by orderNumber
        List<ExamQuestion> examQuestions = examQuestionRepository
                .findByExamIdOrderByOrderNumber(examId);
        
        // Add questions
        for (ExamQuestion examQuestion : examQuestions) {
            QuestionVersion version = examQuestion.getQuestionVersion();
            addQuestion(document, version, examQuestion.getOrderNumber());
            
            // Add answers if requested
            if (includeAnswers) {
                addAnswers(document, version);
            }
        }
        
        // Save document
        String filename = "exam_" + examId + "_" + System.currentTimeMillis() + ".docx";
        Path filePath = exportPath.resolve(filename);
        
        try (FileOutputStream out = new FileOutputStream(filePath.toFile())) {
            document.write(out);
        }
        
        document.close();
        
        return filePath.toString();
    }
    
    /**
     * Add title to document
     */
    private void addTitle(XWPFDocument document, String title) {
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(title);
        titleRun.setBold(true);
        titleRun.setFontSize(18);
        
        // Add empty line
        document.createParagraph();
    }
    
    /**
     * Add question to document
     */
    private void addQuestion(XWPFDocument document, QuestionVersion version, int questionNumber) {
        // Question number and title
        XWPFParagraph questionPara = document.createParagraph();
        XWPFRun questionRun = questionPara.createRun();
        questionRun.setText("Câu " + questionNumber + ": ");
        questionRun.setBold(true);
        
        if (version.getTitle() != null && !version.getTitle().isEmpty()) {
            String title = version.getTitle();
            // Remove "Câu X:" prefix if exists
            if (title.matches("^Câu\\s+\\d+[:.].*")) {
                title = title.replaceFirst("^Câu\\s+\\d+[:.]\\s*", "");
            }
            questionRun.setText("Câu " + questionNumber + ": " + title);
        } else {
            questionRun.setText("Câu " + questionNumber + ": ");
        }
        
        // Question content - Parse và insert text + OMML (dùng OMML gốc)
        if (version.getContentOmml() != null && !version.getContentOmml().isEmpty()) {
            XWPFParagraph contentPara = document.createParagraph();
            addContentToParagraph(contentPara, version.getContentOmml());
        }
        
        // Add images if any
        List<Image> images = imageRepository.findByQuestionVersionId(version.getId());
        for (Image image : images) {
            addImage(document, image.getImagePath());
        }
        
        // Add empty line
        document.createParagraph();
    }
    
    /**
     * Add answers to document
     */
    private void addAnswers(XWPFDocument document, QuestionVersion version) {
        // Get answers from repository
        List<Answer> answers = answerRepository.findByQuestionVersionIdOrderByOrderLabel(version.getId());
        
        if (answers.isEmpty()) {
            return;
        }
        
        // Add answer header
        XWPFParagraph answerHeaderPara = document.createParagraph();
        XWPFRun answerHeaderRun = answerHeaderPara.createRun();
        answerHeaderRun.setText("Đáp án:");
        answerHeaderRun.setBold(true);
        
        // Add each answer
        for (Answer answer : answers) {
            XWPFParagraph answerPara = document.createParagraph();
            answerPara.setIndentationFirstLine(720); // Indent 0.5 inch
            XWPFRun answerRun = answerPara.createRun();
            
            // Mark correct answer
            if (answer.getIsCorrect()) {
                answerRun.setBold(true);
            }
            
            // Add answer label
            if (answer.getOrderLabel() != null && !answer.getOrderLabel().isEmpty()) {
                answerRun.setText(answer.getOrderLabel() + ". ");
            }
            
            // Add answer content (may contain OMML)
            if (answer.getContentOmml() != null && !answer.getContentOmml().isEmpty()) {
                // Create new run for answer content to handle OMML properly
                XWPFRun contentRun = answerPara.createRun();
                addContentToParagraph(answerPara, answer.getContentOmml());
            }
        }
        
        document.createParagraph();
    }
    
    /**
     * Add content (text + OMML) to paragraph
     */
    private void addContentToParagraph(XWPFParagraph para, String content) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        
        // Parse content thành segments
        List<ContentSegment> segments = ContentParser.parse(content);
        
        for (ContentSegment segment : segments) {
            if (segment.isText()) {
                // Add text
                XWPFRun run = para.createRun();
                run.setText(segment.getContent());
            } else if (segment.isOMML()) {
                // Insert OMML equation into paragraph
                boolean success = OMMLInserter.insertOMML(para, segment.getContent());
                if (!success) {
                    // Fallback: Insert placeholder text if OMML insertion fails
                    logger.warn("Failed to insert OMML, using placeholder");
                    XWPFRun run = para.createRun();
                    run.setText("[Equation]");
                }
            }
        }
    }
    
    /**
     * Add image to document
     */
    private void addImage(XWPFDocument document, String imagePath) {
        try {
            logger.debug("Adding image: {}", imagePath);
            Path path = Paths.get(imagePath);
            
            if (!Files.exists(path)) {
                logger.warn("Image file not found: {}", path.toAbsolutePath());
                return;
            }
            
            byte[] imageData = Files.readAllBytes(path);
            if (imageData.length == 0) {
                logger.warn("Image file is empty: {}", imagePath);
                return;
            }
            
            XWPFParagraph imagePara = document.createParagraph();
            imagePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun imageRun = imagePara.createRun();
            
            // Determine image type
            String imageType = "png";
            String lowerPath = imagePath.toLowerCase();
            if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
                imageType = "jpeg";
            } else if (lowerPath.endsWith(".gif")) {
                imageType = "gif";
            }
            
            // Insert image (max width: 500 pixels, maintain aspect ratio)
            // Calculate height based on aspect ratio if needed
            int imageTypeConstant = getImageType(imageType);
            imageRun.addPicture(
                    new java.io.ByteArrayInputStream(imageData),
                    imageTypeConstant,
                    "image",
                    Units.toEMU(500), // max width 500px
                    Units.toEMU(375)  // proportional height (3:4 ratio)
            );
            
            logger.debug("Image added successfully: {}", imagePath);
            document.createParagraph();
        } catch (Exception e) {
            logger.error("Error adding image {}: {}", imagePath, e.getMessage(), e);
            // Don't throw - continue with document creation
        }
    }
    
    /**
     * Get image type constant
     */
    private int getImageType(String imageType) {
        switch (imageType.toLowerCase()) {
            case "jpeg":
            case "jpg":
                return XWPFDocument.PICTURE_TYPE_JPEG;
            case "gif":
                return XWPFDocument.PICTURE_TYPE_GIF;
            case "png":
            default:
                return XWPFDocument.PICTURE_TYPE_PNG;
        }
    }
    
    /**
     * Export random exam to DOCX file (random questions)
     */
    public String exportRandomExamToDOCX(int numberOfQuestions, boolean includeAnswers) throws IOException {
        logger.info("=== exportRandomExamToDOCX started ===");
        logger.info("Parameters: numberOfQuestions={}, includeAnswers={}", numberOfQuestions, includeAnswers);
        logger.info("Export directory: {}", exportDir);
        
        if (numberOfQuestions <= 0) {
            String errorMsg = "Number of questions must be greater than 0";
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        
        // Get all active questions
        logger.info("Fetching all active questions...");
        List<Question> allQuestions = questionRepository.findByIsActiveTrue();
        logger.info("Found {} active questions", allQuestions.size());
        
        if (allQuestions.isEmpty()) {
            String errorMsg = "No active questions found in database";
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        
        // Determine how many questions to select (use all available if less than requested)
        int questionsToSelect = Math.min(numberOfQuestions, allQuestions.size());
        logger.info("Will select {} questions (available: {}, requested: {})", 
                   questionsToSelect, allQuestions.size(), numberOfQuestions);
        
        if (allQuestions.size() < numberOfQuestions) {
            logger.warn("Only {} questions available, less than requested {}. Will use all available questions.", 
                        allQuestions.size(), numberOfQuestions);
        }
        
        // Random select questions
        logger.info("Shuffling and selecting {} questions...", questionsToSelect);
        Collections.shuffle(allQuestions);
        List<Question> selectedQuestions = allQuestions.stream()
                .limit(questionsToSelect)
                .collect(Collectors.toList());
        logger.info("Selected {} questions", selectedQuestions.size());
        
        // Get question versions
        logger.info("Fetching question versions...");
        List<QuestionVersion> questionVersions = new java.util.ArrayList<>();
        int skippedCount = 0;
        for (Question question : selectedQuestions) {
            // Get published version or latest version
            Optional<QuestionVersion> versionOpt = questionVersionRepository
                    .findByQuestionIdAndIsPublishedTrue(question.getId());
            
            if (!versionOpt.isPresent()) {
                logger.debug("No published version for question {}, getting latest version", question.getId());
                List<QuestionVersion> versions = questionVersionRepository.findByQuestionId(question.getId());
                if (!versions.isEmpty()) {
                    versionOpt = Optional.of(versions.get(versions.size() - 1));
                    logger.debug("Using latest version for question {}", question.getId());
                }
            }
            
            if (versionOpt.isPresent()) {
                questionVersions.add(versionOpt.get());
            } else {
                skippedCount++;
                logger.warn("No version found for question {}", question.getId());
            }
        }
        logger.info("Found {} question versions (skipped: {})", questionVersions.size(), skippedCount);
        
        if (questionVersions.isEmpty()) {
            String errorMsg = "No question versions found";
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        
        // Shuffle question versions
        logger.info("Shuffling question versions...");
        Collections.shuffle(questionVersions);
        
        // Create export directory if not exists
        Path exportPath = Paths.get(exportDir);
        logger.info("Checking export directory: {}", exportPath.toAbsolutePath());
        if (!Files.exists(exportPath)) {
            logger.info("Export directory does not exist, creating...");
            Files.createDirectories(exportPath);
            logger.info("Export directory created");
        } else {
            logger.info("Export directory exists");
        }
        
        // Create new DOCX document
        logger.info("Creating DOCX document...");
        XWPFDocument document = new XWPFDocument();
        
        // Add title
        logger.info("Adding title to document...");
        addTitle(document, "Đề Thi Mới");
        
        // Add questions
        logger.info("Adding {} questions to document...", questionVersions.size());
        for (int i = 0; i < questionVersions.size(); i++) {
            QuestionVersion version = questionVersions.get(i);
            logger.debug("Adding question {}: versionId={}", i + 1, version.getId());
            addQuestion(document, version, i + 1);
            
            // Add answers if requested
            if (includeAnswers) {
                addAnswers(document, version);
            }
        }
        logger.info("All questions added to document");
        
        // Save document
        String filename = "exam_random_" + System.currentTimeMillis() + ".docx";
        Path filePath = exportPath.resolve(filename);
        logger.info("Saving document to: {}", filePath.toAbsolutePath());
        
        try (FileOutputStream out = new FileOutputStream(filePath.toFile())) {
            document.write(out);
            logger.info("Document written successfully");
        } catch (Exception e) {
            logger.error("Error writing document: {}", e.getMessage(), e);
            throw e;
        }
        
        document.close();
        logger.info("Document closed");
        
        long fileSize = Files.size(filePath);
        logger.info("File created successfully. Path: {}, Size: {} bytes", filePath.toString(), fileSize);
        logger.info("=== exportRandomExamToDOCX completed ===");
        
        return filePath.toString();
    }
}


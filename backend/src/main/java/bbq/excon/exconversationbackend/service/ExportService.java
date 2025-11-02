package bbq.excon.exconversationbackend.service;

import bbq.excon.exconversationbackend.entity.*;
import bbq.excon.exconversationbackend.repository.AnswerRepository;
import bbq.excon.exconversationbackend.repository.ExamQuestionRepository;
import bbq.excon.exconversationbackend.repository.ExamRepository;
import bbq.excon.exconversationbackend.repository.ImageRepository;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ExportService {
    
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private ExamQuestionRepository examQuestionRepository;
    
    @Autowired
    private ImageRepository imageRepository;
    
    @Autowired
    private AnswerRepository answerRepository;
    
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
        
        // Question content
        if (version.getContent() != null && !version.getContent().isEmpty()) {
            XWPFParagraph contentPara = document.createParagraph();
            XWPFRun contentRun = contentPara.createRun();
            
            // Extract text from content (may contain MathML)
            String content = version.getContent();
            // TODO: Convert MathML back to OMML and insert into document
            // For now, just insert text
            contentRun.setText(content);
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
            
            String answerText = (answer.getOrderLabel() != null ? answer.getOrderLabel() + ". " : "") +
                               (answer.getContent() != null ? answer.getContent() : "");
            answerRun.setText(answerText);
            
            // TODO: Convert MathML back to OMML for answer content
        }
        
        document.createParagraph();
    }
    
    /**
     * Add image to document
     */
    private void addImage(XWPFDocument document, String imagePath) {
        try {
            Path path = Paths.get(imagePath);
            if (!Files.exists(path)) {
                return;
            }
            
            byte[] imageData = Files.readAllBytes(path);
            
            XWPFParagraph imagePara = document.createParagraph();
            imagePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun imageRun = imagePara.createRun();
            
            // Determine image type
            String imageType = "png";
            if (imagePath.toLowerCase().endsWith(".jpg") || imagePath.toLowerCase().endsWith(".jpeg")) {
                imageType = "jpeg";
            } else if (imagePath.toLowerCase().endsWith(".gif")) {
                imageType = "gif";
            }
            
            // Insert image (width: 400 pixels, height: auto)
            imageRun.addPicture(
                    new java.io.ByteArrayInputStream(imageData),
                    getImageType(imageType),
                    "image",
                    Units.toEMU(400),
                    Units.toEMU(300) // height will be scaled proportionally
            );
            
            document.createParagraph();
        } catch (Exception e) {
            // Ignore image errors
            e.printStackTrace();
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
}


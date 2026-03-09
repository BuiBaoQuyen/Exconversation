package bbq.excon.exconversationbackend.parser;

import bbq.excon.exconversationbackend.entity.*;
import bbq.excon.exconversationbackend.repository.*;
import bbq.excon.exconversationbackend.service.MathMLConverterService;
import bbq.excon.exconversationbackend.service.OMMLToMathMLConverterService;
import bbq.excon.exconversationbackend.service.FileStorageService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import static org.apache.poi.xwpf.usermodel.XWPFDocument.PICTURE_TYPE_JPEG;
import static org.apache.poi.xwpf.usermodel.XWPFDocument.PICTURE_TYPE_PNG;
import static org.apache.poi.xwpf.usermodel.XWPFDocument.PICTURE_TYPE_GIF;
import static org.apache.poi.xwpf.usermodel.XWPFDocument.PICTURE_TYPE_BMP;
import static org.apache.poi.xwpf.usermodel.XWPFDocument.PICTURE_TYPE_TIFF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${file.images-dir}")
    private String imagesDir;

    /**
     * Parse Word document và extract questions with chapter detection
     * Async processing để không block request
     * 
     * @param uploadId ID của Upload record
     */
    @Async("aiParseExecutor")
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

                // Get or create default chapter (will be used if no chapter is detected)
                Chapter currentChapter = getOrCreateDefaultChapter(upload.getUploadedByName());

                // Extract paragraphs and create questions
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                System.out.println("Found " + paragraphs.size() + " paragraphs");

                int questionNumber = 0;
                int chapterCount = 0;

                for (XWPFParagraph para : paragraphs) {
                    String paraText = para.getText();
                    if (paraText == null || paraText.trim().isEmpty()) {
                        continue;
                    }

                    // Check if paragraph is a chapter heading
                    ChapterInfo chapterInfo = detectChapter(paraText);
                    if (chapterInfo != null) {
                        chapterCount++;
                        System.out.println(
                                "Detected chapter: " + chapterInfo.name + " (Index: " + chapterInfo.index + ")");

                        // Create or find chapter
                        currentChapter = chapterRepository.findByChapterName(chapterInfo.name)
                                .orElseGet(() -> {
                                    Chapter newChapter = new Chapter();
                                    newChapter.setChapterName(chapterInfo.name);
                                    newChapter.setChapterIndex(chapterInfo.index);
                                    newChapter.setCreatedByName(upload.getUploadedByName());
                                    return chapterRepository.save(newChapter);
                                });
                        continue; // Skip to next paragraph
                    }

                    // Check if paragraph looks like a question
                    if (isQuestionParagraph(paraText)) {
                        questionNumber++;
                        System.out.println("Processing question " + questionNumber + " in chapter: "
                                + currentChapter.getChapterName() +
                                " - " + paraText.substring(0, Math.min(50, paraText.length())));

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
                            question.setChapter(currentChapter);
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

                            // IMPORTANT: Save version FIRST to get valid ID for foreign key references
                            version = questionVersionRepository.save(version);

                            // Extract and save images from this paragraph and following paragraphs.
                            // Returns placeholder string to embed into content (e.g. "[IMAGE:42]").
                            String imagePlaceholders = extractAndSaveImagesWithPlaceholders(
                                    document, para, version, question.getId());

                            // If images were found, append their placeholders into
                            // contentMathml/contentOmml
                            // and save the version again so FE can render images inline at correct
                            // position.
                            if (imagePlaceholders != null && !imagePlaceholders.isEmpty()) {
                                String updatedOmml = (version.getContentOmml() != null
                                        ? version.getContentOmml()
                                        : "") + imagePlaceholders;
                                String updatedMathml = (version.getContentMathml() != null
                                        ? version.getContentMathml()
                                        : "") + imagePlaceholders;
                                version.setContentOmml(updatedOmml);
                                version.setContentMathml(updatedMathml);
                                version = questionVersionRepository.save(version);
                                System.out.println("  [IMAGE PLACEHOLDER] Appended placeholders: " + imagePlaceholders);
                            }

                            // Extract and save answers (A, B, C, D) from following paragraphs
                            int answersFound = extractAndSaveAnswers(document, para, version,
                                    upload.getUploadedByName());

                            System.out.println("Created question " + questionNumber +
                                    " with content length: " + contentOmml.length() +
                                    ", answers: " + answersFound);
                        }
                    }
                }

                // Update status to parsed
                upload.setStatus("parsed");
                upload.setNote("Successfully parsed " + questionNumber + " questions in " +
                        (chapterCount > 0 ? chapterCount + " chapters" : "default chapter"));
                uploadRepository.save(upload);

                System.out.println("Document parsing completed. Created " + questionNumber +
                        " questions in " + (chapterCount > 0 ? chapterCount + " chapters" : "default chapter"));

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
     * Detect chapter heading from paragraph text
     * Supports multiple formats:
     * - "Chương 1: Tên chương", "CHƯƠNG 1", "Chương I"
     * - "Chapter 1: Name", "CHAPTER 1"
     * - "Phần 1", "PHẦN I"
     * - "Chương 1.", "Chương I."
     * 
     * @param text Paragraph text
     * @return ChapterInfo if detected, null otherwise
     */
    private ChapterInfo detectChapter(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String trimmed = text.trim();

        // Pattern 1: "Chương" + number/roman + optional name
        // Examples: "Chương 1", "CHƯƠNG 1: Tên", "Chương I", "Chương I: Tên"
        Pattern chuongPattern = Pattern.compile(
                "^\\s*(?:CHƯƠNG|Chương|chương)\\s+([IVXivx0-9]+)(?:[:\\s.-]+(.+))?\\s*$",
                Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher chuongMatcher = chuongPattern.matcher(trimmed);
        if (chuongMatcher.find()) {
            String indexStr = chuongMatcher.group(1);
            String name = chuongMatcher.group(2);

            int index = parseChapterIndex(indexStr);
            String chapterName = name != null && !name.trim().isEmpty()
                    ? "Chương " + indexStr + ": " + name.trim()
                    : "Chương " + indexStr;

            return new ChapterInfo(index, chapterName);
        }

        // Pattern 2: "Chapter" + number + optional name (English)
        Pattern chapterPattern = Pattern.compile(
                "^\\s*(?:CHAPTER|Chapter|chapter)\\s+([IVXivx0-9]+)(?:[:\\s.-]+(.+))?\\s*$",
                Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher chapterMatcher = chapterPattern.matcher(trimmed);
        if (chapterMatcher.find()) {
            String indexStr = chapterMatcher.group(1);
            String name = chapterMatcher.group(2);

            int index = parseChapterIndex(indexStr);
            String chapterName = name != null && !name.trim().isEmpty()
                    ? "Chapter " + indexStr + ": " + name.trim()
                    : "Chapter " + indexStr;

            return new ChapterInfo(index, chapterName);
        }

        // Pattern 3: "Phần" + number/roman + optional name
        Pattern phanPattern = Pattern.compile(
                "^\\s*(?:PHẦN|Phần|phần)\\s+([IVXivx0-9]+)(?:[:\\s.-]+(.+))?\\s*$",
                Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher phanMatcher = phanPattern.matcher(trimmed);
        if (phanMatcher.find()) {
            String indexStr = phanMatcher.group(1);
            String name = phanMatcher.group(2);

            int index = parseChapterIndex(indexStr);
            String chapterName = name != null && !name.trim().isEmpty()
                    ? "Phần " + indexStr + ": " + name.trim()
                    : "Phần " + indexStr;

            return new ChapterInfo(index, chapterName);
        }

        return null;
    }

    /**
     * Parse chapter index from string (supports Arabic and Roman numerals)
     */
    private int parseChapterIndex(String indexStr) {
        if (indexStr == null || indexStr.trim().isEmpty()) {
            return 0;
        }

        // Try to parse as integer first
        try {
            return Integer.parseInt(indexStr);
        } catch (NumberFormatException e) {
            // Try to parse as Roman numeral
            return parseRomanNumeral(indexStr.toUpperCase());
        }
    }

    /**
     * Parse Roman numeral to integer
     */
    private int parseRomanNumeral(String roman) {
        if (roman == null || roman.isEmpty()) {
            return 0;
        }

        java.util.Map<Character, Integer> romanMap = new java.util.HashMap<>();
        romanMap.put('I', 1);
        romanMap.put('V', 5);
        romanMap.put('X', 10);
        romanMap.put('L', 50);
        romanMap.put('C', 100);
        romanMap.put('D', 500);
        romanMap.put('M', 1000);

        int result = 0;
        int prevValue = 0;

        for (int i = roman.length() - 1; i >= 0; i--) {
            char ch = roman.charAt(i);
            Integer value = romanMap.get(ch);
            if (value == null) {
                return 0; // Invalid Roman numeral
            }

            if (value < prevValue) {
                result -= value;
            } else {
                result += value;
            }
            prevValue = value;
        }

        return result;
    }

    /**
     * Inner class to hold chapter information
     */
    private static class ChapterInfo {
        int index;
        String name;

        ChapterInfo(int index, String name) {
            this.index = index;
            this.name = name;
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
     * Check if paragraph looks like an answer option
     * Patterns: "A.", "B.", "C.", "D.", "A)", "B)", etc.
     */
    private boolean isAnswerParagraph(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // Pattern: Single letter (A-D) followed by dot or parenthesis at start
        Pattern answerPattern = Pattern.compile("^\\s*[A-Da-d]\\s*[.\\)]");
        return answerPattern.matcher(text).find();
    }

    /**
     * Extract answer label from paragraph text
     * Returns: "A", "B", "C", or "D"
     */
    private String extractAnswerLabel(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        Pattern answerPattern = Pattern.compile("^\\s*([A-Da-d])\\s*[.\\)]");
        java.util.regex.Matcher matcher = answerPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return null;
    }

    /**
     * Extract and save answers (A, B, C, D) from paragraphs following a question
     * Returns the number of answers found and saved
     */
    private int extractAndSaveAnswers(XWPFDocument document, XWPFParagraph questionPara,
            QuestionVersion version, String createdByName) {
        try {
            System.out.println("  [ANSWER EXTRACTION] Starting for version ID: " + version.getId());

            // First, try to find answers in tables (common format: 4 columns x 1 row)
            System.out.println("  [ANSWER EXTRACTION] Attempting table-based extraction...");
            int answersFromTable = extractAnswersFromTable(document, questionPara, version);
            if (answersFromTable > 0) {
                System.out.println("  [ANSWER EXTRACTION] ✓ Found " + answersFromTable + " answers in table");
                return answersFromTable;
            }
            System.out.println("  [ANSWER EXTRACTION] No table answers found, trying paragraph-based...");

            // If no table found, look for paragraph-based answers
            List<XWPFParagraph> allParagraphs = document.getParagraphs();
            int currentIndex = allParagraphs.indexOf(questionPara);
            if (currentIndex < 0) {
                return 0;
            }

            int answersFound = 0;
            // Look at the next paragraphs for answers (typically A, B, C, D are within next
            // 10 paragraphs)
            for (int i = 1; i <= 10 && (currentIndex + i) < allParagraphs.size(); i++) {
                XWPFParagraph para = allParagraphs.get(currentIndex + i);
                String paraText = para.getText();

                // Stop if we hit another question
                if (paraText != null && isQuestionParagraph(paraText)) {
                    break;
                }

                // Check if this is an answer paragraph
                if (paraText != null && isAnswerParagraph(paraText)) {
                    String answerLabel = extractAnswerLabel(paraText);
                    if (answerLabel != null) {
                        // Extract content (text + OMML)
                        String contentOmml = mathMLConverterService.extractContentWithMath(para);
                        if (contentOmml != null && !contentOmml.trim().isEmpty()) {
                            // Convert to MathML
                            String contentMathml = ommlToMathMLConverterService.convertContentOMMLToMathML(contentOmml);
                            if (contentMathml != null && contentMathml.trim().isEmpty()) {
                                contentMathml = null;
                            }

                            // Create Answer entity
                            Answer answer = new Answer();
                            answer.setQuestionVersion(version);
                            answer.setOrderLabel(answerLabel);
                            answer.setContentOmml(contentOmml);
                            answer.setContentMathml(contentMathml);
                            answer.setIsCorrect(false); // Default to false, can be updated later
                            answerRepository.save(answer);

                            answersFound++;// Stop after finding 4 answers (A, B, C, D)
                            if (answersFound >= 4) {
                                break;
                            }
                        }
                    }
                }
            }

            System.out.println("  [ANSWER EXTRACTION] Paragraph-based extraction found " + answersFound + " answers");
            return answersFound;
        } catch (Exception e) {
            System.err.println(
                    "[ERROR] Exception in extractAndSaveAnswers for version " + version.getId() + ": "
                            + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Extract answers from table (format: 4 columns x 1 row for A, B, C, D)
     * Returns the number of answers found and saved
     */
    private int extractAnswersFromTable(XWPFDocument document, XWPFParagraph questionPara, QuestionVersion version) {
        try {
            // Get all tables in document
            List<XWPFTable> tables = document.getTables();
            System.out.println("    [TABLE] Total tables in document: " + tables.size());
            if (tables.isEmpty()) {
                System.out.println("    [TABLE] No tables found in document");
                return 0;
            }

            // Find the position of the question paragraph in the document body
            int questionPos = document.getPosOfParagraph(questionPara);
            System.out.println("    [TABLE] Question paragraph position: " + questionPos);
            if (questionPos < 0) {
                System.out.println("    [TABLE] WARNING: Question position is negative, cannot locate table");
                return 0;
            }

            // Look for a table immediately after the question (within next 3 body elements)
            System.out.println(
                    "    [TABLE] Searching for table in positions " + (questionPos + 1) + " to " + (questionPos + 3));
            for (int offset = 1; offset <= 3; offset++) {
                int tablePos = questionPos + offset;

                // Check if this position contains a table
                for (XWPFTable table : tables) {
                    int currentTablePos = document.getPosOfTable(table);
                    if (currentTablePos == tablePos) {
                        // Found a table right after the question
                        System.out.println(
                                "    [TABLE] ✓ Found table at position " + tablePos + " (offset +" + offset + ")");
                        return extractAnswersFromTableRows(table, version);
                    }
                }
            }

            System.out.println("    [TABLE] No table found within 3 positions after question");
            return 0;
        } catch (Exception e) {
            System.err.println("    [TABLE ERROR] Exception: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Extract answers from table rows
     * Handles format: 4 columns in 1 row (A, B, C, D)
     */
    private int extractAnswersFromTableRows(XWPFTable table, QuestionVersion version) {
        try {
            int answersFound = 0;
            String[] labels = { "A", "B", "C", "D" };

            // Check if table has at least 1 row
            System.out.println("      [TABLE ROWS] Table has " + table.getRows().size() + " rows");
            if (table.getRows().isEmpty()) {
                System.out.println("      [TABLE ROWS] Table is empty");
                return 0;
            }

            // Get first row (answers are typically in first row)
            XWPFTableRow row = table.getRows().get(0);
            List<XWPFTableCell> cells = row.getTableCells();
            System.out.println("      [TABLE ROWS] First row has " + cells.size() + " cells");

            // Check if we have 4 columns (A, B, C, D)
            if (cells.size() >= 4) {
                System.out.println("      [TABLE ROWS] Processing first 4 cells as answers A-D");
                // Extract from first 4 cells
                for (int i = 0; i < 4 && i < cells.size(); i++) {
                    XWPFTableCell cell = cells.get(i);

                    // Get all paragraphs in the cell
                    List<XWPFParagraph> cellParas = cell.getParagraphs();
                    if (!cellParas.isEmpty()) {
                        // Combine all paragraphs in the cell
                        StringBuilder cellContent = new StringBuilder();
                        for (XWPFParagraph para : cellParas) {
                            String contentOmml = mathMLConverterService.extractContentWithMath(para);
                            if (contentOmml != null && !contentOmml.trim().isEmpty()) {
                                if (cellContent.length() > 0) {
                                    cellContent.append(" ");
                                }
                                cellContent.append(contentOmml);
                            }
                        }

                        String contentOmml = cellContent.toString().trim();
                        if (!contentOmml.isEmpty()) {
                            // Convert to MathML
                            String contentMathml = ommlToMathMLConverterService.convertContentOMMLToMathML(contentOmml);
                            if (contentMathml != null && contentMathml.trim().isEmpty()) {
                                contentMathml = null;
                            }

                            // Create Answer entity
                            Answer answer = new Answer();
                            answer.setQuestionVersion(version);
                            answer.setOrderLabel(labels[i]);
                            answer.setContentOmml(contentOmml);
                            answer.setContentMathml(contentMathml);
                            answer.setIsCorrect(false);
                            answerRepository.save(answer);

                            answersFound++;
                            System.out.println("  Saved table answer " + labels[i] + " with content length: "
                                    + contentOmml.length());
                        }
                    }
                }
            }

            System.out.println("      [TABLE ROWS] ✓ Successfully extracted " + answersFound + " answers from table");
            return answersFound;
        } catch (Exception e) {
            System.err.println("      [TABLE ROWS ERROR] Exception: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
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

    /**
     * Extract images from paragraph and save them to database.
     * Also checks the next few paragraphs for images.
     * Returns a placeholder string "[IMAGE:{id}]" for each saved image,
     * so caller can embed them into content at the correct position.
     */
    private String extractAndSaveImagesWithPlaceholders(XWPFDocument document, XWPFParagraph para,
            QuestionVersion version, Long questionId) {
        StringBuilder placeholders = new StringBuilder();
        try {
            // Extract images from current paragraph (question paragraph itself)
            List<Image> imagesInPara = extractImagesFromParagraphWithSave(para, version, questionId);
            for (Image img : imagesInPara) {
                placeholders.append("[IMAGE:").append(img.getId()).append("]");
            }

            // Also check next 3 paragraphs for images (images often appear right after
            // question text)
            List<XWPFParagraph> allParagraphs = document.getParagraphs();
            int currentIndex = allParagraphs.indexOf(para);
            if (currentIndex >= 0) {
                for (int i = 1; i <= 3 && (currentIndex + i) < allParagraphs.size(); i++) {
                    XWPFParagraph nextPara = allParagraphs.get(currentIndex + i);
                    String nextParaText = nextPara.getText();
                    // Stop if we hit another question or answer paragraph
                    if (nextParaText != null && isQuestionParagraph(nextParaText)) {
                        break;
                    }
                    List<Image> imagesInNext = extractImagesFromParagraphWithSave(nextPara, version, questionId);
                    for (Image img : imagesInNext) {
                        placeholders.append("[IMAGE:").append(img.getId()).append("]");
                    }
                }
            }
        } catch (Exception e) {
            System.err
                    .println("Error extracting images for question version " + version.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
        return placeholders.toString();
    }

    /**
     * Extract images from paragraph and save them to database.
     * Returns the list of saved Image entities (with ID populated).
     */
    private List<Image> extractImagesFromParagraphWithSave(XWPFParagraph para, QuestionVersion version,
            Long questionId) {
        List<Image> savedImages = new java.util.ArrayList<>();
        try {
            for (XWPFRun run : para.getRuns()) {
                for (XWPFPicture picture : run.getEmbeddedPictures()) {
                    try {
                        XWPFPictureData pictureData = picture.getPictureData();
                        if (pictureData != null) {
                            byte[] imageData = pictureData.getData();
                            if (imageData != null && imageData.length > 0) {
                                String extension = getImageExtension(pictureData.getPictureType());
                                String imageName = "image_" + System.currentTimeMillis() + "_" +
                                        (int) (Math.random() * 1000) + extension;

                                String imagePath = fileStorageService.storeImageFromBytes(
                                        imageData, imageName, questionId);

                                Image image = new Image();
                                image.setQuestionVersion(version);
                                image.setImagePath(imagePath);
                                image.setDescription("Extracted from DOCX");
                                image = imageRepository.save(image); // save to get ID
                                savedImages.add(image);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing picture: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting images from paragraph: " + e.getMessage());
        }
        return savedImages;
    }

    /**
     * Get image file extension from POI picture type
     */
    private String getImageExtension(int pictureType) {
        if (pictureType == PICTURE_TYPE_JPEG) {
            return ".jpg";
        } else if (pictureType == PICTURE_TYPE_PNG) {
            return ".png";
        } else if (pictureType == PICTURE_TYPE_GIF) {
            return ".gif";
        } else if (pictureType == PICTURE_TYPE_BMP) {
            return ".bmp";
        } else if (pictureType == PICTURE_TYPE_TIFF) {
            return ".tiff";
        } else {
            return ".png"; // Default to PNG
        }
    }
}

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

                        // Extract content from the question paragraph itself (text + OMML)
                        String firstParaOmml = mathMLConverterService.extractContentWithMath(para);

                        if (firstParaOmml != null && !firstParaOmml.trim().isEmpty()) {
                            // Create Question entity
                            Question question = new Question();
                            question.setChapter(currentChapter);
                            question.setType("Trắc nghiệm");
                            question.setCreatedByName(upload.getUploadedByName());
                            question.setIsActive(true);
                            question = questionRepository.save(question);

                            // IMPORTANT: Save version FIRST to get valid ID for foreign key references
                            QuestionVersion version = new QuestionVersion();
                            version.setQuestion(question);
                            version.setVersionNumber(1);
                            version.setTitle("Câu " + questionNumber);
                            version.setCreatedByName(upload.getUploadedByName());
                            version.setIsPublished(false);
                            version = questionVersionRepository.save(version);

                            // Collect all body paragraphs that belong to this question
                            // (paragraphs between the question line and the first answer/next question)
                            List<XWPFParagraph> bodyParagraphs = extractQuestionBodyParagraphs(
                                    document, para);

                            // Build combined content (OMML & MathML) from question paragraph
                            // and all body paragraphs, inserting [IMAGE:id] at the correct position
                            String[] combinedContent = buildQuestionContent(
                                    firstParaOmml, para, bodyParagraphs, version, question.getId());

                            String contentOmml = combinedContent[0];
                            String contentMathml = combinedContent[1];

                            version.setContentOmml(contentOmml);
                            version.setContentMathml(contentMathml);
                            version = questionVersionRepository.save(version);

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
            // Store only the descriptive name; fall back to "Chương X" if none
            String chapterName = name != null && !name.trim().isEmpty()
                    ? name.trim()
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
            // Store only the descriptive name; fall back to "Chapter X" if none
            String chapterName = name != null && !name.trim().isEmpty()
                    ? name.trim()
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
            // Store only the descriptive name; fall back to "Phần X" if none
            String chapterName = name != null && !name.trim().isEmpty()
                    ? name.trim()
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
     * Collect all paragraphs that belong to the body of a question.
     * Scanning starts from the paragraph AFTER the question paragraph and stops
     * when a new question, a chapter heading, or an answer option is detected.
     *
     * @param document  the full DOCX document
     * @param questionPara  the paragraph that was identified as a question start
     * @return ordered list of body paragraphs (may contain text and/or images)
     */
    private List<XWPFParagraph> extractQuestionBodyParagraphs(XWPFDocument document,
            XWPFParagraph questionPara) {
        List<XWPFParagraph> bodyParagraphs = new java.util.ArrayList<>();
        List<XWPFParagraph> allParagraphs = document.getParagraphs();
        int startIndex = allParagraphs.indexOf(questionPara);
        if (startIndex < 0) {
            return bodyParagraphs;
        }
        for (int i = startIndex + 1; i < allParagraphs.size(); i++) {
            XWPFParagraph next = allParagraphs.get(i);
            String text = next.getText();
            // Stop at a new question, chapter heading, or answer option
            if (text != null && !text.trim().isEmpty()) {
                if (isQuestionParagraph(text) || isAnswerParagraph(text)
                        || detectChapter(text) != null) {
                    break;
                }
            }
            bodyParagraphs.add(next);
        }
        return bodyParagraphs;
    }

    /**
     * Build combined OMML and MathML content for a question by merging:
     * 1. The content of the question paragraph itself.
     * 2. The content of each body paragraph in order, inserting [IMAGE:id]
     *    placeholders at the exact position where images appear.
     *
     * @param firstParaOmml  OMML content already extracted from the question paragraph
     * @param questionPara   the question paragraph (may also contain inline images)
     * @param bodyParagraphs paragraphs after the question line that still belong to it
     * @param version        the QuestionVersion entity (must already be persisted)
     * @param questionId     parent question ID (used when saving image files)
     * @return String[2]: [0] = combined OMML string, [1] = combined MathML string
     */
    private String[] buildQuestionContent(String firstParaOmml, XWPFParagraph questionPara,
            List<XWPFParagraph> bodyParagraphs, QuestionVersion version, Long questionId) {
        StringBuilder ommlBuilder = new StringBuilder();
        StringBuilder mathmlBuilder = new StringBuilder();

        // --- Part 1: Question paragraph itself ---
        // Strip the "Câu X." / "Câu X " prefix from the raw OMML so that the
        // content field holds only the question body (the title field carries the number)
        String strippedFirstParaOmml = stripQuestionPrefix(firstParaOmml);
        ommlBuilder.append(strippedFirstParaOmml);
        String firstParaMathml = ommlToMathMLConverterService.convertContentOMMLToMathML(strippedFirstParaOmml);
        if (firstParaMathml == null || firstParaMathml.trim().isEmpty()) {
            firstParaMathml = strippedFirstParaOmml; // fall back to OMML if conversion fails
        }
        mathmlBuilder.append(firstParaMathml);

        // Append any inline images from the question paragraph
        List<Image> imagesInQuestion = extractImagesFromParagraphWithSave(questionPara, version, questionId);
        for (Image img : imagesInQuestion) {
            String placeholder = "[IMAGE:" + img.getId() + "]";
            ommlBuilder.append(placeholder);
            mathmlBuilder.append(placeholder);
            System.out.println("  [IMAGE PLACEHOLDER] Inserted from question para: " + placeholder);
        }

        // --- Part 2: Body paragraphs (text or images between question line and answers) ---
        for (XWPFParagraph bodyPara : bodyParagraphs) {
            // First, check for images in this paragraph and insert placeholders
            List<Image> imagesInBody = extractImagesFromParagraphWithSave(bodyPara, version, questionId);
            for (Image img : imagesInBody) {
                String placeholder = "[IMAGE:" + img.getId() + "]";
                ommlBuilder.append(placeholder);
                mathmlBuilder.append(placeholder);
                System.out.println("  [IMAGE PLACEHOLDER] Inserted from body para: " + placeholder);
            }

            // Then append any text/OMML content from this paragraph
            String bodyOmml = mathMLConverterService.extractContentWithMath(bodyPara);
            if (bodyOmml != null && !bodyOmml.trim().isEmpty()) {
                ommlBuilder.append(" ").append(bodyOmml);
                String bodyMathml = ommlToMathMLConverterService.convertContentOMMLToMathML(bodyOmml);
                if (bodyMathml == null || bodyMathml.trim().isEmpty()) {
                    bodyMathml = bodyOmml;
                }
                mathmlBuilder.append(" ").append(bodyMathml);
                System.out.println("  [BODY TEXT] Appended body paragraph text: "
                        + bodyOmml.substring(0, Math.min(60, bodyOmml.length())));
            }
        }

        return new String[] { ommlBuilder.toString(), mathmlBuilder.toString() };
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
     * Remove the question-number prefix from content so that only the body is stored.
     * Examples of removed prefixes: "Câu 1. ", "Câu 12: ", "Question 3. ", "3. "
     * The prefix detection is intentionally lenient so it still works even when
     * whitespace or punctuation varies slightly.
     *
     * @param content raw content string (OMML or plain text) that may start with a prefix
     * @return content with the leading question prefix stripped and trimmed
     */
    private String stripQuestionPrefix(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        // Matches: "Câu 12. ", "Câu 12: ", "Câu 12 ", "Question 3. ", "3. ", "12) " etc.
        String stripped = content.replaceFirst(
                "^\\s*(?:(?:C[aâ]u|Question|Câu)\\s+\\d+|\\d+)\\s*[.:\\-)]\\s*",
                "");
        return stripped.trim().isEmpty() ? content : stripped;
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

package bbq.excon.exconversationbackend.service;

import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service để extract text + OMML từ Word document paragraphs
 * 
 * Extract content từ XWPFParagraph, bao gồm:
 * - Text thuần từ runs
 * - OMML (Office Math Markup Language) từ runs hoặc paragraph level
 * - Combine theo đúng thứ tự xuất hiện trong DOCX
 * 
 * Format output: "Cho hàm số <omml>...</omml>, (a, b, c ∈ R)"
 */
@Service
public class MathMLConverterService {

    /**
     * Extract content từ paragraph (text + OMML equations)
     * Giữ nguyên thứ tự như trong DOCX bằng cách parse XML trực tiếp
     * 
     * Strategy: Parse paragraph XML để đảm bảo text và OMML xuất hiện đúng thứ tự
     * DOCX structure:
     * <w:p><w:r><w:t>text</w:t></w:r><m:oMath>...</m:oMath><w:r>...</w:r></w:p>
     * 
     * @param para Paragraph từ Word document
     * @return Combined content với text và OMML expressions theo đúng thứ tự
     */
    public String extractContentWithMath(XWPFParagraph para) {
        if (para == null) {
            return "";
        }

        StringBuilder content = new StringBuilder();

        try {
            // Strategy: Parse paragraph XML trực tiếp để giữ nguyên thứ tự
            // DOCX structure:
            // <w:p><w:r><w:t>text</w:t></w:r><m:oMath>...</m:oMath><w:r>...</w:r></w:p>
            // Parse XML để đảm bảo text và OMML xuất hiện đúng thứ tự như trong DOCX

            CTP ctp = para.getCTP();
            XmlCursor cursor = ctp.newCursor();

            try {
                // Namespaces
                String wNamespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
                String mNamespace = "http://schemas.openxmlformats.org/officeDocument/2006/math";

                // Strategy: Parse XML để lấy tất cả child elements theo thứ tự
                // DOCX structure: <w:p><w:r>...</w:r><m:oMath>...</m:oMath><w:r>...</w:r></w:p>

                // IMPORTANT: Cursor is already at paragraph (CTP) level
                // Don't use toStartDoc() - it moves to document root!
                // Just select child elements directly

                // Try different XPath patterns to find child elements
                String[] xpathPatterns = {
                        "declare namespace w='" + wNamespace + "' declare namespace m='" + mNamespace
                                + "' ./w:r | ./m:oMath",
                        "declare namespace w='" + wNamespace + "' declare namespace m='" + mNamespace + "' ./child::*",
                        "./w:r | ./m:oMath",
                        "./*"
                };

                int elementCount = 0;
                boolean foundElements = false;

                for (String xpath : xpathPatterns) {
                    try {
                        // Create new cursor for each attempt to ensure we're at paragraph level
                        XmlCursor testCursor = ctp.newCursor();
                        try {
                            // Select child elements directly (cursor is already at paragraph level)
                            testCursor.selectPath(xpath);
                            elementCount = 0;
                            content.setLength(0); // Reset content

                            while (testCursor.toNextSelection()) {
                                elementCount++;
                                XmlObject obj = testCursor.getObject();
                                if (obj == null)
                                    continue;

                                String localName = obj.getDomNode().getLocalName();
                                String namespaceURI = obj.getDomNode().getNamespaceURI();
                                if (namespaceURI != null && namespaceURI.equals(mNamespace)
                                        && "oMath".equals(localName)) {
                                    // This is an OMML element - extract it at this position
                                    String ommlXml = obj.xmlText();
                                    if (ommlXml != null && !ommlXml.trim().isEmpty()) {
                                        String cleanedOMML = bbq.excon.exconversationbackend.service.omml.OMMLNormalizer
                                                .cleanOMML(ommlXml);
                                        if (cleanedOMML != null && !cleanedOMML.trim().isEmpty()) {
                                            content.append("<omml>").append(cleanedOMML).append("</omml>");
                                        }
                                    }
                                } else if (namespaceURI != null && namespaceURI.equals(wNamespace)
                                        && "r".equals(localName)) {
                                    // This is a run - extract text from it at this position
                                    String runText = extractTextFromRunXML(obj);
                                    if (runText != null && !runText.trim().isEmpty()) {
                                        content.append(runText);
                                    }
                                }
                            }

                            if (elementCount > 0 && content.length() > 0) {
                                foundElements = true;
                                break; // Success, exit loop
                            }
                        } finally {
                            testCursor.close();
                        }
                    } catch (Exception e) {
                        System.err.println("DEBUG: XPath pattern failed: " + xpath + " - " + e.getMessage());
                        e.printStackTrace();
                        continue; // Try next pattern
                    }
                }
                // Nếu không tìm thấy elements qua bất kỳ XPath nào, thử fallback method
                if (!foundElements || elementCount == 0 || content.length() == 0) {
                    return extractContentWithMathFallback(para);
                }
            } finally {
                cursor.close();
            }

        } catch (Exception e) {
            // Fallback to old method if XML parsing fails
            System.err.println("Error in XML-based extraction, using fallback: " + e.getMessage());
            e.printStackTrace();
            return extractContentWithMathFallback(para);
        }

        String result = content.toString();
        if (result.isEmpty()) {
            return extractContentWithMathFallback(para);
        }
        return result;
    }

    /**
     * Extract text từ run XML object
     * 
     * @param runObj XML object của run
     * @return Text content từ run
     */
    private String extractTextFromRunXML(XmlObject runObj) {
        try {
            String wNamespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
            XmlCursor cursor = runObj.newCursor();
            StringBuilder text = new StringBuilder();

            try {
                // Find all <w:t> elements in the run
                cursor.selectPath("declare namespace w='" + wNamespace + "' .//w:t");
                while (cursor.toNextSelection()) {
                    String textContent = cursor.getTextValue();
                    if (textContent != null) {
                        text.append(textContent);
                    }
                }
            } finally {
                cursor.close();
            }

            return text.toString();
        } catch (Exception e) {
            System.err.println("Error extracting text from run XML: " + e.getMessage());
            return "";
        }
    }

    /**
     * Fallback method: Extract content using runs + parse XML để tìm OMML ở đúng vị
     * trí
     * Used when primary XML parsing fails
     * Strategy: Parse XML một lần nữa với cách tiếp cận khác để tìm OMML ở đúng vị
     * trí
     */
    private String extractContentWithMathFallback(XWPFParagraph para) {
        StringBuilder content = new StringBuilder();

        try {
            // Strategy: Parse XML để build ordered list của text và OMML
            CTP ctp = para.getCTP();
            XmlCursor cursor = ctp.newCursor();

            try {
                String wNamespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
                String mNamespace = "http://schemas.openxmlformats.org/officeDocument/2006/math";

                // Strategy: Parse XML để lấy tất cả elements theo thứ tự
                // Build ordered list: collect all elements with their positions
                List<OrderedContent> orderedItems = new ArrayList<>();

                // Parse XML để lấy tất cả child elements theo thứ tự
                // IMPORTANT: cursor từ ctp.newCursor() đã ở paragraph level
                // KHÔNG dùng toStartDoc() - nó sẽ đưa cursor về document root!

                // Try different XPath patterns
                String[] xpathPatterns = {
                        "declare namespace w='" + wNamespace + "' declare namespace m='" + mNamespace
                                + "' ./w:r | ./m:oMath",
                        "declare namespace w='" + wNamespace + "' declare namespace m='" + mNamespace + "' ./child::*",
                        "./w:r | ./m:oMath",
                        "./*"
                };

                boolean parsed = false;
                for (String xpath : xpathPatterns) {
                    try {
                        // Create new cursor to ensure we're at paragraph level
                        XmlCursor testCursor = ctp.newCursor();
                        try {
                            testCursor.selectPath(xpath);

                            int position = 0;
                            while (testCursor.toNextSelection()) {
                                XmlObject obj = testCursor.getObject();
                                if (obj == null) {
                                    position += 10;
                                    continue;
                                }

                                String localName = obj.getDomNode().getLocalName();
                                String namespaceURI = obj.getDomNode().getNamespaceURI();

                                if (namespaceURI != null && namespaceURI.equals(wNamespace) && "r".equals(localName)) {
                                    // This is a run - extract text and OMML from it
                                    String runText = extractTextFromRunXML(obj);
                                    if (runText != null && !runText.trim().isEmpty()) {
                                        orderedItems.add(new OrderedContent(position, runText, false));
                                        position += 1;
                                    }

                                    // Check for OMML in this run
                                    try {
                                        XmlCursor runCursor = obj.newCursor();
                                        try {
                                            String ommlXpath = "declare namespace m='" + mNamespace + "' .//m:oMath";
                                            runCursor.selectPath(ommlXpath);
                                            if (runCursor.toNextSelection()) {
                                                XmlObject mathObject = runCursor.getObject();
                                                if (mathObject != null) {
                                                    String ommlXml = mathObject.xmlText();
                                                    if (ommlXml != null && !ommlXml.trim().isEmpty()) {
                                                        String cleanedOMML = bbq.excon.exconversationbackend.service.omml.OMMLNormalizer
                                                                .cleanOMML(ommlXml);
                                                        if (cleanedOMML != null && !cleanedOMML.trim().isEmpty()) {
                                                            orderedItems.add(
                                                                    new OrderedContent(position, cleanedOMML, true));
                                                            position += 1;
                                                        }
                                                    }
                                                }
                                            }
                                        } finally {
                                            runCursor.close();
                                        }
                                    } catch (Exception e) {
                                        // Ignore
                                    }
                                } else if (namespaceURI != null && namespaceURI.equals(mNamespace)
                                        && "oMath".equals(localName)) {
                                    // This is a paragraph-level OMML element
                                    String ommlXml = obj.xmlText();
                                    if (ommlXml != null && !ommlXml.trim().isEmpty()) {
                                        String cleanedOMML = bbq.excon.exconversationbackend.service.omml.OMMLNormalizer
                                                .cleanOMML(ommlXml);
                                        if (cleanedOMML != null && !cleanedOMML.trim().isEmpty()) {
                                            orderedItems.add(new OrderedContent(position, cleanedOMML, true));
                                            position += 1;
                                        }
                                    }
                                }

                                position += 10;
                            }

                            if (orderedItems.size() > 0) {
                                parsed = true;
                                break; // Success, exit loop
                            }
                        } finally {
                            testCursor.close();
                        }
                    } catch (Exception e) {
                        System.err.println("DEBUG: Fallback XPath failed: " + xpath);
                        continue;
                    }
                }

                if (!parsed) {
                    // If XML parsing failed, fall back to simple run extraction
                    for (XWPFRun run : para.getRuns()) {
                        try {
                            String runText = run.getText(0);
                            if (runText != null) {
                                content.append(runText);
                            }
                        } catch (Exception e) {
                            // Skip
                        }
                    }
                    String result = content.toString();
                    return result;
                }

                // All elements have been collected in the first pass above
                // No need for second pass

                // Sort by position and build final content
                orderedItems.sort((a, b) -> Integer.compare(a.position, b.position));

                for (OrderedContent item : orderedItems) {
                    if (item.isOMML) {
                        content.append("<omml>").append(item.content).append("</omml>");
                    } else {
                        content.append(item.content);
                    }
                }

            } finally {
                cursor.close();
            }

        } catch (Exception e) {
            // Final fallback: extract from runs only
            System.err.println("Error in fallback XML parsing: " + e.getMessage());
            try {
                for (XWPFRun run : para.getRuns()) {
                    String runText = run.getText(0);
                    if (runText != null) {
                        content.append(runText);
                    }
                }
            } catch (Exception ex) {
                String fallbackText = para.getText();
                if (fallbackText != null) {
                    return fallbackText;
                }
            }
        }

        String result = content.toString();
        return result;
    }

    /**
     * Helper class để track content với vị trí và loại
     */
    private static class OrderedContent {
        int position;
        String content;
        boolean isOMML;

        OrderedContent(int position, String content, boolean isOMML) {
            this.position = position;
            this.content = content;
            this.isOMML = isOMML;
        }
    }
}

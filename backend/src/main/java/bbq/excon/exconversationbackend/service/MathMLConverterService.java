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
 * - OMML (Office Math Markup Language) từ runs
 * - Combine theo đúng thứ tự xuất hiện
 * 
 * Format output: "Cho hàm số <omml>...</omml>, (a, b, c ∈ R)"
 */
@Service
public class MathMLConverterService {
    
    /**
     * Extract content từ paragraph (text + OMML equations)
     * 
     * Strategy: Extract theo thứ tự xuất hiện trong paragraph
     * 1. Duyệt qua runs theo thứ tự
     * 2. Với mỗi run:
     *    - Nếu có OMML → extract và wrap trong <omml> tags
     *    - Nếu không → extract text
     * 3. Combine theo đúng thứ tự để giữ context
     * 
     * Format output: "Text <omml>OMML XML</omml> Text"
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
            // Strategy: Extract theo thứ tự xuất hiện trong paragraph
            // Duyệt qua runs theo thứ tự và extract text + OMML tại đúng vị trí
            
            for (XWPFRun run : para.getRuns()) {
                try {
                    String runText = run.getText(0);
                    boolean hasText = (runText != null && !runText.trim().isEmpty());
                    boolean hasMathInRun = hasOMML(run);
                    
                    if (hasMathInRun) {
                        // Run có OMML - extract và wrap trong <omml> tags
                        String ommlXml = extractOMMLFromRun(run);
                        System.out.println("DEBUG: Run has OMML, extracted: " + 
                                         (ommlXml != null ? ommlXml.substring(0, Math.min(100, ommlXml.length())) : "null"));
                        
                        if (ommlXml != null && !ommlXml.trim().isEmpty()) {
                            // Nếu run cũng có text, append text trước rồi mới đến OMML
                            if (hasText && runText != null) {
                                content.append(runText);
                            }
                            // Wrap OMML trong <omml> tags
                            content.append("<omml>").append(ommlXml).append("</omml>");
                        } else if (hasText && runText != null) {
                            // Nếu extract OMML thất bại nhưng có text, dùng text
                            content.append(runText);
                        }
                    } else if (hasText && runText != null) {
                        // Run chỉ có text, không có OMML
                        content.append(runText);
                    }
                } catch (Exception e) {
                    // Fallback: try to extract text if OMML extraction fails
                    try {
                        String runText = run.getText(0);
                        if (runText != null && !runText.trim().isEmpty()) {
                            content.append(runText);
                        }
                    } catch (Exception ex) {
                        // Skip this run if both methods fail
                        System.err.println("Error extracting from run: " + ex.getMessage());
                    }
                }
            }
            
            // Check for OMML at paragraph level (not in runs)
            List<String> paragraphOMML = extractOMMLFromParagraph(para);
            if (!paragraphOMML.isEmpty()) {
                // OMML ở paragraph level - append vào cuối (fallback)
                // Note: Thường OMML đã được extract từ runs ở trên
                for (String omml : paragraphOMML) {
                    if (omml != null && !omml.trim().isEmpty()) {
                        // Clean OMML để giảm kích thước
                        String cleanedOMML = bbq.excon.exconversationbackend.service.omml.OMMLNormalizer.cleanOMML(omml);
                        if (cleanedOMML != null && !cleanedOMML.trim().isEmpty()) {
                            // Check if this OMML was already extracted from runs
                            if (!content.toString().contains("<omml>" + cleanedOMML + "</omml>")) {
                                content.append("<omml>").append(cleanedOMML).append("</omml>");
                                System.out.println("DEBUG: Added paragraph-level OMML");
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            // Fallback to old method if sequential extraction fails
            System.err.println("Error in sequential extraction, using fallback: " + e.getMessage());
            e.printStackTrace();
            return extractContentWithMathFallback(para);
        }
        
        String result = content.toString();
        System.out.println("DEBUG: Final extracted content: " + 
                         result.substring(0, Math.min(200, result.length())));
        return result;
    }
    
    /**
     * Check if a run contains OMML (Office Math Markup Language)
     * 
     * @param run XWPFRun to check
     * @return true if run contains OMML, false otherwise
     */
    private boolean hasOMML(XWPFRun run) {
        if (run == null || run.getCTR() == null) {
            return false;
        }
        
        try {
            String ommlNamespace = "http://schemas.openxmlformats.org/officeDocument/2006/math";
            XmlCursor cursor = run.getCTR().newCursor();
            try {
                String xpath = "declare namespace m='" + ommlNamespace + "' .//m:oMath";
                cursor.selectPath(xpath);
                return cursor.toNextSelection();
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            // If error checking, assume no OMML
            return false;
        }
    }
    
    /**
     * Extract OMML từ một run cụ thể
     * 
     * @param run XWPFRun chứa OMML
     * @return OMML XML string hoặc empty string nếu không có OMML
     */
    private String extractOMMLFromRun(XWPFRun run) {
        if (run == null || run.getCTR() == null) {
            return "";
        }
        
        try {
            String ommlNamespace = "http://schemas.openxmlformats.org/officeDocument/2006/math";
            XmlCursor cursor = run.getCTR().newCursor();
            StringBuilder ommlContent = new StringBuilder();
            
            try {
                String xpath = "declare namespace m='" + ommlNamespace + "' .//m:oMath";
                cursor.selectPath(xpath);
                
                while (cursor.toNextSelection()) {
                    XmlObject mathObject = cursor.getObject();
                    if (mathObject != null) {
                        String ommlXml = mathObject.xmlText();
                        if (ommlXml != null && !ommlXml.trim().isEmpty()) {
                            // Clean OMML để giảm kích thước storage
                            String cleanedOMML = bbq.excon.exconversationbackend.service.omml.OMMLNormalizer.cleanOMML(ommlXml);
                            if (cleanedOMML != null && !cleanedOMML.trim().isEmpty()) {
                                if (ommlContent.length() > 0) {
                                    ommlContent.append(" ");
                                }
                                ommlContent.append(cleanedOMML);
                            }
                        }
                    }
                }
            } finally {
                cursor.close();
            }
            
            return ommlContent.toString();
        } catch (Exception e) {
            System.err.println("Error extracting OMML from run: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Extract OMML từ paragraph level (not in runs)
     * 
     * @param para Paragraph từ Word document
     * @return List of OMML XML strings
     */
    private List<String> extractOMMLFromParagraph(XWPFParagraph para) {
        List<String> ommlExpressions = new ArrayList<>();
        
        try {
            // Access underlying XML structure
            CTP ctp = para.getCTP();
            
            // OMML namespace
            String ommlNamespace = "http://schemas.openxmlformats.org/officeDocument/2006/math";
            
            // Find all math elements (m:oMath) in the paragraph
            XmlCursor cursor = ctp.newCursor();
            try {
                // Chỉ tìm OMML trong phạm vi paragraph hiện tại
                String xpath = "declare namespace m='" + ommlNamespace + "' .//m:oMath";
                cursor.selectPath(xpath);
                
                while (cursor.toNextSelection()) {
                    XmlObject mathObject = cursor.getObject();
                    if (mathObject != null) {
                        String ommlXml = mathObject.xmlText();
                        if (ommlXml != null && !ommlXml.trim().isEmpty()) {
                            ommlExpressions.add(ommlXml);
                        }
                    }
                }
            } finally {
                cursor.close();
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting OMML from paragraph: " + e.getMessage());
        }
        
        return ommlExpressions;
    }
    
    /**
     * Fallback method: Extract content using simple approach
     * Used when sequential extraction fails
     */
    private String extractContentWithMathFallback(XWPFParagraph para) {
        StringBuilder content = new StringBuilder();
        
        // Extract text từ runs
        try {
            for (XWPFRun run : para.getRuns()) {
                String runText = run.getText(0);
                if (runText != null) {
                    content.append(runText);
                }
            }
        } catch (Exception e) {
            // Fallback to para.getText() if error
            String fallbackText = para.getText();
            if (fallbackText != null) {
                return fallbackText;
            }
        }
        
        // Extract và add OMML từ paragraph level
        List<String> paragraphOMML = extractOMMLFromParagraph(para);
        for (String omml : paragraphOMML) {
            if (omml != null && !omml.trim().isEmpty()) {
                // Clean OMML để giảm kích thước
                String cleanedOMML = bbq.excon.exconversationbackend.service.omml.OMMLNormalizer.cleanOMML(omml);
                if (cleanedOMML != null && !cleanedOMML.trim().isEmpty()) {
                    content.append("<omml>").append(cleanedOMML).append("</omml>");
                }
            }
        }
        
        return content.toString();
    }
}


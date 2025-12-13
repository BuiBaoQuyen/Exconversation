package bbq.excon.exconversationbackend.service.export;

import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.officeDocument.x2006.math.CTOMath;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;

/**
 * Helper class để insert OMML equations vào Word document
 */
public class OMMLInserter {
    
    private static final Logger logger = LoggerFactory.getLogger(OMMLInserter.class);
    
    private static final String OMML_NAMESPACE = "http://schemas.openxmlformats.org/officeDocument/2006/math";
    private static final String W_NAMESPACE = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    
    /**
     * Insert OMML XML vào paragraph (tạo run mới với OMML)
     * 
     * @param para XWPFParagraph để insert OMML vào
     * @param ommlXml OMML XML string (có thể không có namespace)
     * @return true nếu thành công, false nếu có lỗi
     */
    public static boolean insertOMML(XWPFParagraph para, String ommlXml) {
        if (para == null || ommlXml == null || ommlXml.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Ensure OMML has proper namespace
            String ommlWithNamespace = ensureNamespace(ommlXml);
            
            // Parse OMML XML
            CTOMath ctOMath = CTOMath.Factory.parse(ommlWithNamespace);
            
            // Get underlying CTP object
            CTP ctp = para.getCTP();
            
            // Create a new run and insert OMML into it using XmlCursor
            XmlCursor cursor = ctp.newCursor();
            try {
                // Move to end of paragraph
                cursor.toEndToken();
                // Create new run element
                QName runQName = new QName(W_NAMESPACE, "r", "w");
                cursor.beginElement(runQName);
                // Move into the run
                cursor.toFirstChild();
                // Insert OMML using QName
                QName oMathQName = new QName(OMML_NAMESPACE, "oMath", "m");
                cursor.beginElement(oMathQName);
                // Copy OMML content
                XmlCursor ommlCursor = ctOMath.newCursor();
                try {
                    ommlCursor.toStartDoc();
                    cursor.copyXml(ommlCursor);
                } finally {
                    ommlCursor.close();
                }
                cursor.toParent();
            } finally {
                cursor.close();
            }
            
            logger.debug("Successfully inserted OMML into paragraph");
            return true;
            
        } catch (XmlException e) {
            logger.error("Error parsing OMML XML: {}", e.getMessage());
            logger.debug("OMML XML: {}", ommlXml.substring(0, Math.min(200, ommlXml.length())));
            return false;
        } catch (Exception e) {
            logger.error("Error inserting OMML into paragraph: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Ensure OMML XML has proper namespace
     * OMML có thể đã được clean (không có namespace), cần thêm lại
     */
    private static String ensureNamespace(String ommlXml) {
        if (ommlXml == null || ommlXml.trim().isEmpty()) {
            return ommlXml;
        }
        
        // Check if namespace already exists
        if (ommlXml.contains("xmlns:m=") || ommlXml.contains("xmlns=\"http://schemas.openxmlformats.org/officeDocument/2006/math\"")) {
            return ommlXml;
        }
        
        // Add namespace to <m:oMath> tag
        String trimmed = ommlXml.trim();
        if (trimmed.startsWith("<m:oMath")) {
            // Has <m:oMath> tag, add namespace to it
            return ommlXml.replaceFirst(
                "<m:oMath(\\s|>)",
                "<m:oMath xmlns:m=\"" + OMML_NAMESPACE + "\"$1"
            );
        } else if (trimmed.startsWith("<oMath")) {
            // Has <oMath> without namespace prefix, add m: prefix and namespace
            return ommlXml.replaceFirst(
                "<oMath(\\s|>)",
                "<m:oMath xmlns:m=\"" + OMML_NAMESPACE + "\"$1"
            ).replaceAll("</oMath>", "</m:oMath>");
        } else {
            // Wrap với <m:oMath> nếu chưa có
            return "<m:oMath xmlns:m=\"" + OMML_NAMESPACE + "\">" + ommlXml + "</m:oMath>";
        }
    }
}


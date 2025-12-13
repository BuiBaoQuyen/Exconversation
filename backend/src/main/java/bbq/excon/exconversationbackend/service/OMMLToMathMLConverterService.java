package bbq.excon.exconversationbackend.service;

import bbq.excon.exconversationbackend.service.omml.OMMLNode;
import bbq.excon.exconversationbackend.service.omml.OMMLToMathMLConverter;
import bbq.excon.exconversationbackend.service.omml.OMMLTreeParser;
import org.springframework.stereotype.Service;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service để convert OMML (Office Math Markup Language) sang MathML
 * Sử dụng thư viện docx4j để thực hiện conversion
 */
@Service
public class OMMLToMathMLConverterService {
    
    /**
     * Convert một fragment OMML XML sang MathML XML
     * 
     * @param ommlXml OMML XML string
     * @return MathML XML string
     */
    public String convertOMMLToMathML(String ommlXml) {
        if (ommlXml == null || ommlXml.trim().isEmpty()) {
            return "";
        }
        
        try {
            // Đảm bảo OMML có namespace đúng
            String normalizedOMML = ensureOMMLNamespaces(ommlXml);
            
            // Thử sử dụng XSLT từ docx4j trước
            String mathml = convertUsingXSLT(normalizedOMML);
            
            // Nếu XSLT fail hoặc không tìm thấy, sử dụng OMMLToMathMLConverter
            if (mathml == null || mathml.trim().isEmpty() || 
                mathml.contains("Warning: OMML2MML.xslt not found")) {
                System.out.println("Using OMMLToMathMLConverter for conversion");
                mathml = convertUsingOMMLConverter(normalizedOMML);
            }
            
            if (mathml != null && !mathml.trim().isEmpty()) {
                return cleanMathML(mathml);
            }
            
            return "";
        } catch (Exception e) {
            System.err.println("Error converting OMML to MathML: " + e.getMessage());
            e.printStackTrace();
            // Fallback: try OMMLToMathMLConverter
            try {
                return convertUsingOMMLConverter(ensureOMMLNamespaces(ommlXml));
            } catch (Exception ex) {
                System.err.println("Fallback conversion also failed: " + ex.getMessage());
                return "";
            }
        }
    }
    
    /**
     * Convert OMML sang MathML sử dụng OMMLToMathMLConverter (implementation thực sự)
     */
    private String convertUsingOMMLConverter(String ommlXml) throws Exception {
        try {
            // Parse OMML XML thành OMMLNode tree
            OMMLNode root = OMMLTreeParser.parse(ommlXml);
            
            if (root == null) {
                return "";
            }
            
            // Convert OMMLNode tree sang MathML
            String mathml = OMMLToMathMLConverter.convert(root);
            
            return mathml;
        } catch (Exception e) {
            System.err.println("Error in OMMLToMathMLConverter: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Convert toàn bộ content có thẻ <omml>...</omml> sang MathML
     * Format: "Text <omml>...</omml> Text" → "Text <math>...</math> Text"
     * 
     * @param content Content có chứa <omml> tags
     * @return Content với <math> tags thay thế
     */
    public String convertContentOMMLToMathML(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        // Pattern để tìm các thẻ <omml>...</omml>
        Pattern pattern = Pattern.compile("<omml>(.*?)</omml>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String ommlXml = matcher.group(1);
            System.out.println("DEBUG: Extracted OMML from <omml> tag, length: " + ommlXml.length());
            System.out.println("DEBUG: OMML preview: " + ommlXml.substring(0, Math.min(200, ommlXml.length())));
            
            String mathml = convertOMMLToMathML(ommlXml);
            
            if (mathml != null && !mathml.trim().isEmpty()) {
                System.out.println("DEBUG: Converted to MathML, length: " + mathml.length());
                System.out.println("DEBUG: MathML preview: " + mathml.substring(0, Math.min(300, mathml.length())));
                
                // MathML đã có <math> tag từ OMMLToMathMLConverter, chỉ cần đảm bảo có namespace
                String wrappedMathML = wrapMathML(mathml);
                matcher.appendReplacement(result, Matcher.quoteReplacement(wrappedMathML));
            } else {
                System.err.println("WARNING: Failed to convert OMML to MathML, keeping original OMML");
                // Nếu convert fail, giữ nguyên OMML
                matcher.appendReplacement(result, matcher.group(0));
            }
        }
        
        matcher.appendTail(result);
        String finalResult = result.toString();
        System.out.println("DEBUG: Final converted content length: " + finalResult.length());
        return finalResult;
    }
    
    /**
     * Convert OMML sang MathML sử dụng XSLT transformation
     * docx4j sử dụng XSLT file OMML2MML.xslt để convert
     */
    private String convertUsingXSLT(String ommlXml) throws Exception {
        try {
            // Load XSLT từ docx4j JAR
            // OMML2MML.xslt thường nằm trong docx4j resources
            InputStream xsltStream = null;
            
            // Thử các vị trí có thể có XSLT file
            // Priority 1: File trong resources folder của project
            // Priority 2: File trong docx4j JAR
            // Hỗ trợ cả .xslt và .xsl (uppercase/lowercase)
            String[] possiblePaths = {
                "/OMML2MML.xslt",  // Trong src/main/resources/
                "/OMML2MML.xsl",   // Hỗ trợ .xsl extension
                "/OMML2MML.XSL",   // Hỗ trợ uppercase
                "/OMML2MML.XSLT",  // Hỗ trợ uppercase
                "OMML2MML.xslt",   // Relative path
                "OMML2MML.xsl",    // Relative path với .xsl
                "org/docx4j/convert/out/mathml/OMML2MML.xslt",
                "org/docx4j/openpackaging/parts/relationships/OMML2MML.xslt",
                "/org/docx4j/convert/out/mathml/OMML2MML.xslt"
            };
            
            for (String path : possiblePaths) {
                // Thử load từ classpath (resources folder)
                xsltStream = getClass().getClassLoader().getResourceAsStream(path);
                if (xsltStream == null) {
                    // Thử load từ class của service
                    xsltStream = getClass().getResourceAsStream(path);
                }
                if (xsltStream == null) {
                    // Thử load với leading slash
                    if (!path.startsWith("/")) {
                        xsltStream = getClass().getClassLoader().getResourceAsStream("/" + path);
                    }
                }
                if (xsltStream != null) {
                    System.out.println("DEBUG: Found OMML2MML.xslt at path: " + path);
                    break;
                }
            }
            
            // Nếu không tìm thấy trong classpath, thử load từ docx4j package
            if (xsltStream == null) {
                try {
                    // Sử dụng docx4j internal XSLT nếu có
                    Class<?> xsltClass = Class.forName("org.docx4j.convert.out.mathml.OMML2MML");
                    xsltStream = xsltClass.getResourceAsStream("OMML2MML.xslt");
                } catch (ClassNotFoundException e) {
                    // Ignore
                }
            }
            
            if (xsltStream == null) {
                // Nếu không tìm thấy XSLT, log chi tiết và return null để fallback
                System.out.println("Warning: OMML2MML.xslt not found in any of the following paths:");
                for (String path : possiblePaths) {
                    System.out.println("  - " + path);
                }
                System.out.println("Will use OMMLToMathMLConverter as fallback.");
                System.out.println("To fix: Download OMML2MML.xslt and place it in src/main/resources/OMML2MML.xslt");
                return null; // Return null để trigger fallback
            }
            
            // Tạo transformer từ XSLT
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            StreamSource xsltSource = new StreamSource(xsltStream);
            Transformer transformer = transformerFactory.newTransformer(xsltSource);
            
            // Set output properties
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
            
            // Transform OMML sang MathML
            StringReader ommlReader = new StringReader(ommlXml);
            StreamSource ommlSource = new StreamSource(ommlReader);
            
            StringWriter mathmlWriter = new StringWriter();
            StreamResult result = new StreamResult(mathmlWriter);
            
            transformer.transform(ommlSource, result);
            
            String mathml = mathmlWriter.toString();
            xsltStream.close();
            
            return mathml;
        } catch (Exception e) {
            System.err.println("XSLT conversion failed: " + e.getMessage());
            e.printStackTrace();
            // Return null để fallback về OMMLToMathMLConverter
            return null;
        }
    }
    
    /**
     * Đảm bảo OMML có namespace đúng
     */
    private String ensureOMMLNamespaces(String ommlXml) {
        String result = ommlXml.trim();
        
        // Kiểm tra xem đã có namespace chưa
        boolean hasM = result.contains("xmlns:m=") || result.contains("xmlns:m=\"");
        boolean hasW = result.contains("xmlns:w=") || result.contains("xmlns:w=\"");
        
        if (hasM && hasW) {
            return result;
        }
        
        // Thêm namespace nếu thiếu
        if (result.startsWith("<m:oMath")) {
            StringBuilder ns = new StringBuilder("<m:oMath");
            if (!hasM) {
                ns.append(" xmlns:m=\"http://schemas.openxmlformats.org/officeDocument/2006/math\"");
            }
            if (!hasW) {
                ns.append(" xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"");
            }
            ns.append("$1");
            return result.replaceFirst("<m:oMath(\\s|>)", ns.toString());
        }
        
        // Wrap fragment
        StringBuilder prefix = new StringBuilder("<m:oMath");
        if (!hasM) {
            prefix.append(" xmlns:m=\"http://schemas.openxmlformats.org/officeDocument/2006/math\"");
        }
        if (!hasW) {
            prefix.append(" xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"");
        }
        prefix.append(">");
        return prefix + result + "</m:oMath>";
    }
    
    /**
     * Wrap MathML trong <math> tag với namespace
     */
    private String wrapMathML(String mathml) {
        if (mathml == null || mathml.trim().isEmpty()) {
            return "";
        }
        
        String trimmed = mathml.trim();
        
        // Nếu đã có <math> tag, chỉ đảm bảo có namespace
        if (trimmed.startsWith("<math")) {
            if (!trimmed.contains("xmlns=")) {
                return trimmed.replaceFirst("<math(\\s|>)", 
                    "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"$1");
            }
            return trimmed;
        }
        
        // Nếu chưa có <math> tag, wrap nó
        return "<math xmlns=\"http://www.w3.org/1998/Math/MathML\">" + trimmed + "</math>";
    }
    
    /**
     * Clean MathML output
     */
    private String cleanMathML(String mathml) {
        if (mathml == null) {
            return null;
        }
        
        String cleaned = mathml.trim();
        
        // Remove XML declaration nếu có
        cleaned = cleaned.replaceFirst("^<\\?xml[^>]*\\?>", "");
        
        // Remove extra whitespace
        cleaned = cleaned.replaceAll("\\s+", " ");
        cleaned = cleaned.replaceAll(">\\s+<", "><");
        
        return cleaned.trim();
    }
}


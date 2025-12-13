package bbq.excon.exconversationbackend.service.omml;

/**
 * Normalize OMML input để xử lý các vấn đề về encoding, spacing, và formatting
 * 
 * Xử lý:
 * - Unicode normalization (zero-width characters, invisible characters)
 * - Math italic → normal text conversion
 * - Spacing normalization
 * - XML entity decoding
 * 
 * @author AI Assistant
 */
public class OMMLNormalizer {
    
    // Mapping math italic → normal characters
    private static final String[] MATH_ITALIC_START = {
        "\uD835\uDC9C", "\uD835\uDC9D", "\uD835\uDC9E", "\uD835\uDC9F", "\uD835\uDCA0",
        "\uD835\uDCA1", "\uD835\uDCA2", "\uD835\uDCA3", "\uD835\uDCA4", "\uD835\uDCA5",
        "\uD835\uDCA6", "\uD835\uDCA7", "\uD835\uDCA8", "\uD835\uDCA9", "\uD835\uDCAA",
        "\uD835\uDCAB", "\uD835\uDCAC", "\uD835\uDCAD", "\uD835\uDCAE", "\uD835\uDCAF",
        "\uD835\uDCB0", "\uD835\uDCB1", "\uD835\uDCB2", "\uD835\uDCB3", "\uD835\uDCB4",
        "\uD835\uDCB5"
    };
    
    private static final char[] NORMAL_CHARS = {
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
    };
    
    /**
     * Normalize OMML XML string
     * 
     * @param ommlXml Raw OMML XML string
     * @return Normalized OMML XML string
     */
    public static String normalize(String ommlXml) {
        if (ommlXml == null || ommlXml.isEmpty()) {
            return ommlXml;
        }
        
        String normalized = ommlXml;
        
        // Step 1: Decode XML entities
        normalized = decodeXmlEntities(normalized);
        
        // Step 2: Remove zero-width characters
        normalized = removeZeroWidthCharacters(normalized);
        
        // Step 3: Normalize whitespace (nhưng giữ structure trong XML tags)
        normalized = normalizeWhitespace(normalized);
        
        // Step 4: Convert math italic to normal text
        normalized = convertMathItalicToNormal(normalized);
        
        // Step 5: Fix common encoding issues
        normalized = fixEncodingIssues(normalized);
        
        return normalized;
    }
    
    /**
     * Decode XML entities
     */
    private static String decodeXmlEntities(String text) {
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace("&#xA0;", " ");
    }
    
    /**
     * Remove zero-width characters và invisible characters
     */
    private static String removeZeroWidthCharacters(String text) {
        // Remove zero-width space, zero-width non-joiner, zero-width joiner
        return text
            .replace("\u200B", "")  // Zero-width space
            .replace("\u200C", "")  // Zero-width non-joiner
            .replace("\u200D", "")  // Zero-width joiner
            .replace("\uFEFF", "")  // Zero-width no-break space
            .replace("\u2060", "")  // Word joiner
            .replace("\u200E", "")  // Left-to-right mark
            .replace("\u200F", ""); // Right-to-left mark
    }
    
    /**
     * Normalize whitespace (giữ structure trong XML tags)
     */
    private static String normalizeWhitespace(String text) {
        // Chỉ normalize whitespace bên ngoài XML tags
        // Strategy: Split by XML tags, normalize text parts, keep tags unchanged
        StringBuilder result = new StringBuilder();
        boolean inTag = false;
        StringBuilder currentText = new StringBuilder();
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '<') {
                // Normalize accumulated text before entering tag
                if (currentText.length() > 0) {
                    result.append(normalizeTextWhitespace(currentText.toString()));
                    currentText.setLength(0);
                }
                inTag = true;
                result.append(c);
            } else if (c == '>') {
                inTag = false;
                result.append(c);
            } else if (inTag) {
                // Inside tag, keep as is
                result.append(c);
            } else {
                // Outside tag, accumulate for normalization
                currentText.append(c);
            }
        }
        
        // Normalize remaining text
        if (currentText.length() > 0) {
            result.append(normalizeTextWhitespace(currentText.toString()));
        }
        
        return result.toString();
    }
    
    /**
     * Normalize whitespace trong text content
     */
    private static String normalizeTextWhitespace(String text) {
        // Replace multiple spaces with single space
        // Replace tabs, newlines with space
        return text
            .replaceAll("[\\s]+", " ")
            .trim();
    }
    
    /**
     * Convert math italic characters (Unicode Mathematical Alphanumeric Symbols) to normal text
     */
    private static String convertMathItalicToNormal(String text) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean converted = false;
            
            // Check if character is in math italic range
            if (Character.isHighSurrogate(c) && i + 1 < text.length()) {
                char low = text.charAt(i + 1);
                if (Character.isLowSurrogate(low)) {
                    String surrogate = String.valueOf(new char[]{c, low});
                    
                    // Check if it's math italic
                    for (int j = 0; j < MATH_ITALIC_START.length; j++) {
                        if (surrogate.equals(MATH_ITALIC_START[j])) {
                            result.append(NORMAL_CHARS[j]);
                            i++; // Skip low surrogate
                            converted = true;
                            break;
                        }
                    }
                }
            }
            
            if (!converted) {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Fix common encoding issues
     */
    private static String fixEncodingIssues(String text) {
        // Fix common Unicode issues
        return text
            .replace("\u2013", "-")  // En dash
            .replace("\u2014", "--") // Em dash
            .replace("\u2018", "'")  // Left single quotation mark
            .replace("\u2019", "'")  // Right single quotation mark
            .replace("\u201C", "\"") // Left double quotation mark
            .replace("\u201D", "\"") // Right double quotation mark
            .replace("\u2026", "...") // Horizontal ellipsis
            .replace("\u00A0", " ");  // Non-breaking space
    }
    
    /**
     * Normalize text content từ OMML node
     * Áp dụng normalization cho text content trước khi convert sang LaTeX
     */
    public static String normalizeText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String normalized = text;
        
        // Remove zero-width characters
        normalized = removeZeroWidthCharacters(normalized);
        
        // Convert math italic
        normalized = convertMathItalicToNormal(normalized);
        
        // Fix encoding
        normalized = fixEncodingIssues(normalized);
        
        // Normalize whitespace
        normalized = normalizeTextWhitespace(normalized);
        
        return normalized;
    }
    
    /**
     * Clean OMML XML để giảm kích thước storage
     * Loại bỏ namespace declarations không cần thiết, chỉ giữ phần cốt lõi
     * 
     * Strategy:
     * 1. Extract chỉ <m:oMath>...</m:oMath> từ OMML XML
     * 2. Loại bỏ <xml-fragment> và các namespace declarations
     * 3. Loại bỏ namespace attributes không cần thiết trong tags
     * 4. Normalize whitespace
     * 
     * @param ommlXml Raw OMML XML string (có thể chứa xml-fragment và nhiều namespaces)
     * @return Cleaned OMML XML string (chỉ <m:oMath>...</m:oMath>)
     */
    public static String cleanOMML(String ommlXml) {
        if (ommlXml == null || ommlXml.trim().isEmpty()) {
            return ommlXml;
        }
        
        String cleaned = ommlXml;
        
        // Step 1: Loại bỏ <xml-fragment> tags và các namespace declarations
        // Pattern: <xml-fragment xmlns:...>...</xml-fragment> → extract content inside
        java.util.regex.Pattern xmlFragmentPattern = java.util.regex.Pattern.compile(
            "<xml-fragment[^>]*>(.*?)</xml-fragment>",
            java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher fragmentMatcher = xmlFragmentPattern.matcher(cleaned);
        if (fragmentMatcher.find()) {
            // Extract content inside xml-fragment
            cleaned = fragmentMatcher.group(1);
        }
        
        // Step 2: Extract chỉ <m:oMath>...</m:oMath> nếu có
        java.util.regex.Pattern oMathPattern = java.util.regex.Pattern.compile(
            "<m:oMath[^>]*>(.*?)</m:oMath>",
            java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher oMathMatcher = oMathPattern.matcher(cleaned);
        if (oMathMatcher.find()) {
            // Extract content và wrap lại với <m:oMath> đơn giản
            String oMathContent = oMathMatcher.group(1);
            cleaned = "<m:oMath>" + oMathContent + "</m:oMath>";
        } else {
            // Nếu không có <m:oMath>, tìm các m: tags khác
            // Giữ nguyên nhưng loại bỏ namespace attributes
            cleaned = removeNamespaceAttributes(cleaned);
        }
        
        // Step 3: Loại bỏ namespace attributes từ các tags
        cleaned = removeNamespaceAttributes(cleaned);
        
        // Step 4: Normalize whitespace trong XML
        cleaned = normalizeXmlWhitespace(cleaned);
        
        return cleaned.trim();
    }
    
    /**
     * Loại bỏ namespace attributes từ XML tags
     * Pattern: xmlns:xxx="..." → remove
     */
    private static String removeNamespaceAttributes(String xml) {
        if (xml == null || xml.isEmpty()) {
            return xml;
        }
        
        // Pattern để tìm và loại bỏ namespace attributes
        // xmlns:xxx="..." hoặc xmlns="..."
        java.util.regex.Pattern namespacePattern = java.util.regex.Pattern.compile(
            "\\s+xmlns(?::[^=]+)?=\"[^\"]*\"",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        
        return namespacePattern.matcher(xml).replaceAll("");
    }
    
    /**
     * Normalize whitespace trong XML (giữ structure nhưng giảm kích thước)
     */
    private static String normalizeXmlWhitespace(String xml) {
        if (xml == null || xml.isEmpty()) {
            return xml;
        }
        
        // Loại bỏ whitespace thừa giữa các tags
        // Pattern: >\s+< → ><
        xml = xml.replaceAll(">\\s+<", "><");
        
        // Loại bỏ whitespace thừa trong tags (sau attributes)
        // Pattern: \s+> → >
        xml = xml.replaceAll("\\s+>", ">");
        
        // Loại bỏ whitespace thừa trước closing tags
        // Pattern: \s+</ → </
        xml = xml.replaceAll("\\s+</", "</");
        
        return xml;
    }
}


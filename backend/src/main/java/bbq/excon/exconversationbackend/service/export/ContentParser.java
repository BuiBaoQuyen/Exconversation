package bbq.excon.exconversationbackend.service.export;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Parser để tách content thành text segments và OMML segments
 * Content format: "Text <omml>OMML_XML</omml> Text"
 */
public class ContentParser {
    
    // Pattern để tìm <omml>...</omml> tags
    private static final Pattern OMML_PATTERN = Pattern.compile(
        "<omml>(.*?)</omml>",
        Pattern.DOTALL
    );
    
    /**
     * Parse content string thành list of segments
     * 
     * @param content Content string có thể chứa <omml>...</omml> tags
     * @return List of ContentSegment (TEXT hoặc OMML)
     */
    public static List<ContentSegment> parse(String content) {
        List<ContentSegment> segments = new ArrayList<>();
        
        if (content == null || content.trim().isEmpty()) {
            return segments;
        }
        
        Matcher matcher = OMML_PATTERN.matcher(content);
        int lastEnd = 0;
        
        while (matcher.find()) {
            // Add text before OMML
            int ommlStart = matcher.start();
            if (ommlStart > lastEnd) {
                String textBefore = content.substring(lastEnd, ommlStart);
                if (!textBefore.trim().isEmpty()) {
                    segments.add(new ContentSegment(ContentSegment.Type.TEXT, textBefore));
                }
            }
            
            // Add OMML
            String ommlXml = matcher.group(1);
            if (ommlXml != null && !ommlXml.trim().isEmpty()) {
                segments.add(new ContentSegment(ContentSegment.Type.OMML, ommlXml.trim()));
            }
            
            lastEnd = matcher.end();
        }
        
        // Add remaining text after last OMML
        if (lastEnd < content.length()) {
            String textAfter = content.substring(lastEnd);
            if (!textAfter.trim().isEmpty()) {
                segments.add(new ContentSegment(ContentSegment.Type.TEXT, textAfter));
            }
        }
        
        // If no OMML found, add entire content as text
        if (segments.isEmpty()) {
            segments.add(new ContentSegment(ContentSegment.Type.TEXT, content));
        }
        
        return segments;
    }
}


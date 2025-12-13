package bbq.excon.exconversationbackend.service;

import bbq.excon.exconversationbackend.service.omml.OMMLNode;
import bbq.excon.exconversationbackend.service.omml.OMMLToLaTeXConverter;
import bbq.excon.exconversationbackend.service.omml.OMMLTreeParser;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converter OMML → LaTeX, thân thiện MathJax/KaTeX.
 * (Giữ tên lớp/method để tương thích code gọi hiện có.)
 */
@Service
public class OMMLToMathMLConverter {
    
    /** Entry point: convert một fragment OMML sang LaTeX (chưa wrap delimiters). */
    public String convertOMMLToMathML(String ommlXml) {
        if (ommlXml == null || ommlXml.trim().isEmpty()) return "";
        try {
            String normalized = ensureNamespaces(ommlXml);
            OMMLNode root = parseXml(normalized);
            if (root == null) return "";
            String latex = mapOmmlToLatex(root);
            return cleanAndFinalize(latex);
        } catch (Exception e) {
            System.err.println("Error converting OMML to LaTeX: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }
    
    /** Convert toàn bộ content có thẻ <omml>...</omml> sang LaTeX, giữ nguyên phần text khác. */
    public String convertContentOMMLToMathML(String content) {
        if (content == null || content.trim().isEmpty()) return content;

        Pattern pattern = Pattern.compile("<omml>(.*?)</omml>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String ommlXml = matcher.group(1);
            String latex = convertOMMLToMathML(ommlXml);
            if (latex != null && !latex.trim().isEmpty()) {
                String wrapped = "\\(" + latex.trim() + "\\)"; // inline MathJax
                matcher.appendReplacement(result, Matcher.quoteReplacement(wrapped));
            } else {
                matcher.appendReplacement(result, matcher.group(0)); // giữ nguyên OMML khi fail
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    // -------------------- Utility pipeline --------------------

    /** parse_xml: đảm bảo namespace m/w, trả về OMMLNode tree. */
    private OMMLNode parseXml(String ommlXml) throws Exception {
        return OMMLTreeParser.parse(ommlXml);
    }

    /** map_omml_tag: dùng converter OMMLToLaTeXConverter để map toàn bộ tree sang LaTeX. */
    private String mapOmmlToLatex(OMMLNode root) {
        return OMMLToLaTeXConverter.convert(root);
    }

    /** clean_and_finalize: lọc lỗi thường gặp, cân bằng ngoặc, chuẩn hóa R. */
    private String cleanAndFinalize(String latex) {
        if (latex == null) return null;
        String cleaned = latex.trim();

        // placeholder lỗi
        if (cleaned.contains("?")) return null;

        // chuẩn hóa ký hiệu tập R
        cleaned = cleaned.replace("\\inR", "\\in \\mathbb{R}");

        // cân bằng ngoặc { }
        long open = cleaned.chars().filter(c -> c == '{').count();
        long close = cleaned.chars().filter(c -> c == '}').count();
        if (open != close) return null;

        return cleaned;
    }

    /** Đảm bảo có xmlns:m và xmlns:w để parser không lỗi prefix. */
    private String ensureNamespaces(String ommlXml) {
        String result = ommlXml.trim();
        boolean hasM = result.contains("xmlns:m=");
        boolean hasW = result.contains("xmlns:w=");

        // đủ namespace
        if (hasM && hasW) return result;

        // root là <m:oMath ...>
        if (result.startsWith("<m:oMath")) {
            StringBuilder ns = new StringBuilder("<m:oMath");
            if (!hasM) ns.append(" xmlns:m=\"http://schemas.openxmlformats.org/officeDocument/2006/math\"");
            if (!hasW) ns.append(" xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"");
            ns.append("$1");
            return result.replaceFirst("<m:oMath(\\s|>)", ns.toString());
        }

        // wrap fragment
        StringBuilder prefix = new StringBuilder("<m:oMath");
        if (!hasM) prefix.append(" xmlns:m=\"http://schemas.openxmlformats.org/officeDocument/2006/math\"");
        if (!hasW) prefix.append(" xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"");
        prefix.append(">");
        return prefix + result + "</m:oMath>";
    }
}


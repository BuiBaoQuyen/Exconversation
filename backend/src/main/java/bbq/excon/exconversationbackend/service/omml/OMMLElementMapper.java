package bbq.excon.exconversationbackend.service.omml;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapping OMML elements sang LaTeX commands
 * 
 * Bảng ánh xạ đầy đủ các OMML elements → LaTeX
 * Hỗ trợ: fractions, radicals, superscripts, subscripts, operators, 
 *         matrices, delimiters, accents, functions, etc.
 * 
 * @author AI Assistant
 */
public class OMMLElementMapper {
    
    // OMML namespace
    public static final String OMML_NAMESPACE = "http://schemas.openxmlformats.org/officeDocument/2006/math";
    
    // Mapping OMML tag names (without namespace prefix) to LaTeX
    private static final Map<String, String> TAG_TO_LATEX = new HashMap<>();
    
    // Mapping operators
    private static final Map<String, String> OPERATOR_MAP = new HashMap<>();
    
    // Mapping functions
    private static final Map<String, String> FUNCTION_MAP = new HashMap<>();
    
    static {
        initializeMappings();
    }
    
    /**
     * Initialize all mappings
     */
    private static void initializeMappings() {
        // Basic structure tags
        TAG_TO_LATEX.put("oMath", "");  // Root element, no LaTeX equivalent
        TAG_TO_LATEX.put("r", "");      // Run, handled separately
        TAG_TO_LATEX.put("t", "");      // Text, handled separately
        
        // Fractions
        TAG_TO_LATEX.put("f", "\\frac");  // Fraction
        TAG_TO_LATEX.put("num", "");      // Numerator (handled in converter)
        TAG_TO_LATEX.put("den", "");      // Denominator (handled in converter)
        
        // Radicals
        TAG_TO_LATEX.put("rad", "\\sqrt"); // Radical
        TAG_TO_LATEX.put("deg", "");       // Degree (for nth root)
        TAG_TO_LATEX.put("e", "");         // Expression (handled in converter)
        
        // Superscripts and Subscripts
        TAG_TO_LATEX.put("sup", "^");      // Superscript
        TAG_TO_LATEX.put("sub", "_");      // Subscript
        TAG_TO_LATEX.put("sSup", "^");     // Superscript (alternative)
        TAG_TO_LATEX.put("sSub", "_");     // Subscript (alternative)
        
        // Matrices and Arrays
        TAG_TO_LATEX.put("m", "matrix");   // Matrix
        TAG_TO_LATEX.put("mr", "");        // Matrix row
        TAG_TO_LATEX.put("mc", "");        // Matrix column
        TAG_TO_LATEX.put("mPr", "");       // Matrix properties
        
        // Delimiters
        TAG_TO_LATEX.put("d", "");         // Delimiter
        TAG_TO_LATEX.put("dPr", "");       // Delimiter properties
        TAG_TO_LATEX.put("begChr", "");    // Begin character
        TAG_TO_LATEX.put("endChr", "");    // End character
        TAG_TO_LATEX.put("sepChr", "");    // Separator character
        
        // Group characters (large brackets, etc.)
        TAG_TO_LATEX.put("groupChr", "");  // Group character
        TAG_TO_LATEX.put("groupChrPr", ""); // Group character properties
        
        // Operators
        TAG_TO_LATEX.put("nary", "");      // N-ary operator (∑, ∏, ∫)
        TAG_TO_LATEX.put("naryPr", "");    // N-ary properties
        TAG_TO_LATEX.put("sub", "");       // Subscript (for operators)
        TAG_TO_LATEX.put("sup", "");       // Superscript (for operators)
        TAG_TO_LATEX.put("e", "");         // Expression (for operators)
        
        // Accents
        TAG_TO_LATEX.put("acc", "");       // Accent
        TAG_TO_LATEX.put("accPr", "");     // Accent properties
        TAG_TO_LATEX.put("chr", "");       // Character (for accent)
        
        // Bars and overlines
        TAG_TO_LATEX.put("bar", "");       // Bar
        TAG_TO_LATEX.put("barPr", "");     // Bar properties
        
        // Limits
        TAG_TO_LATEX.put("lim", "");       // Limit
        TAG_TO_LATEX.put("limLow", "");    // Lower limit
        TAG_TO_LATEX.put("limUpp", "");    // Upper limit
        
        // Equation arrays
        TAG_TO_LATEX.put("eqArr", "");     // Equation array
        TAG_TO_LATEX.put("eqArrPr", "");   // Equation array properties
        
        // Phantoms (invisible elements)
        TAG_TO_LATEX.put("phant", "");     // Phantom
        TAG_TO_LATEX.put("phantPr", "");   // Phantom properties
        
        // Boxes
        TAG_TO_LATEX.put("box", "");       // Box
        TAG_TO_LATEX.put("boxPr", "");     // Box properties
        
        // Border boxes
        TAG_TO_LATEX.put("borderBox", ""); // Border box
        TAG_TO_LATEX.put("borderBoxPr", ""); // Border box properties
        
        // Initialize operator mappings
        initializeOperators();
        
        // Initialize function mappings
        initializeFunctions();
    }
    
    /**
     * Initialize operator mappings
     */
    private static void initializeOperators() {
        OPERATOR_MAP.put("∑", "\\sum");
        OPERATOR_MAP.put("∏", "\\prod");
        OPERATOR_MAP.put("∫", "\\int");
        OPERATOR_MAP.put("∮", "\\oint");
        OPERATOR_MAP.put("∬", "\\iint");
        OPERATOR_MAP.put("∭", "\\iiint");
        OPERATOR_MAP.put("∐", "\\coprod");
        OPERATOR_MAP.put("⋀", "\\bigwedge");
        OPERATOR_MAP.put("⋁", "\\bigvee");
        OPERATOR_MAP.put("⋂", "\\bigcap");
        OPERATOR_MAP.put("⋃", "\\bigcup");
        OPERATOR_MAP.put("⨀", "\\bigodot");
        OPERATOR_MAP.put("⨁", "\\bigoplus");
        OPERATOR_MAP.put("⨂", "\\bigotimes");
        OPERATOR_MAP.put("⨄", "\\biguplus");
    }
    
    /**
     * Initialize function mappings
     */
    private static void initializeFunctions() {
        FUNCTION_MAP.put("sin", "\\sin");
        FUNCTION_MAP.put("cos", "\\cos");
        FUNCTION_MAP.put("tan", "\\tan");
        FUNCTION_MAP.put("cot", "\\cot");
        FUNCTION_MAP.put("sec", "\\sec");
        FUNCTION_MAP.put("csc", "\\csc");
        FUNCTION_MAP.put("arcsin", "\\arcsin");
        FUNCTION_MAP.put("arccos", "\\arccos");
        FUNCTION_MAP.put("arctan", "\\arctan");
        FUNCTION_MAP.put("sinh", "\\sinh");
        FUNCTION_MAP.put("cosh", "\\cosh");
        FUNCTION_MAP.put("tanh", "\\tanh");
        FUNCTION_MAP.put("ln", "\\ln");
        FUNCTION_MAP.put("log", "\\log");
        FUNCTION_MAP.put("exp", "\\exp");
        FUNCTION_MAP.put("min", "\\min");
        FUNCTION_MAP.put("max", "\\max");
        FUNCTION_MAP.put("sup", "\\sup");
        FUNCTION_MAP.put("inf", "\\inf");
        FUNCTION_MAP.put("lim", "\\lim");
        FUNCTION_MAP.put("limsup", "\\limsup");
        FUNCTION_MAP.put("liminf", "\\liminf");
        FUNCTION_MAP.put("gcd", "\\gcd");
        FUNCTION_MAP.put("lcm", "\\lcm");
        FUNCTION_MAP.put("det", "\\det");
        FUNCTION_MAP.put("dim", "\\dim");
        FUNCTION_MAP.put("ker", "\\ker");
        FUNCTION_MAP.put("hom", "\\hom");
        FUNCTION_MAP.put("arg", "\\arg");
    }
    
    /**
     * Get LaTeX command for OMML tag
     */
    public static String getLaTeXForTag(String tagName) {
        // Remove namespace prefix if present
        String tag = tagName;
        if (tag.contains(":")) {
            tag = tag.substring(tag.indexOf(":") + 1);
        }
        return TAG_TO_LATEX.getOrDefault(tag, "");
    }
    
    /**
     * Check if tag is a known OMML element
     */
    public static boolean isKnownOMMLElement(String tagName) {
        String tag = tagName;
        if (tag.contains(":")) {
            tag = tag.substring(tag.indexOf(":") + 1);
        }
        return TAG_TO_LATEX.containsKey(tag);
    }
    
    /**
     * Get LaTeX for operator
     */
    public static String getLaTeXForOperator(String operator) {
        return OPERATOR_MAP.getOrDefault(operator, operator);
    }
    
    /**
     * Get LaTeX for function
     */
    public static String getLaTeXForFunction(String function) {
        return FUNCTION_MAP.getOrDefault(function.toLowerCase(), function);
    }
    
    /**
     * Check if text is a known function
     */
    public static boolean isKnownFunction(String text) {
        return FUNCTION_MAP.containsKey(text.toLowerCase());
    }
    
    /**
     * Get delimiter LaTeX based on character
     */
    public static String getDelimiterLaTeX(String delimiter) {
        switch (delimiter) {
            case "(": return "\\left(";
            case ")": return "\\right)";
            case "[": return "\\left[";
            case "]": return "\\right]";
            case "{": return "\\left\\{";
            case "}": return "\\right\\}";
            case "|": return "\\left|";
            case "‖": return "\\left\\|";
            case "⌊": return "\\left\\lfloor";
            case "⌋": return "\\right\\rfloor";
            case "⌈": return "\\left\\lceil";
            case "⌉": return "\\right\\rceil";
            case "⟨": return "\\left\\langle";
            case "⟩": return "\\right\\rangle";
            default: return delimiter;
        }
    }
    
    /**
     * Get matrix environment based on properties
     */
    public static String getMatrixEnvironment(String matrixType) {
        if (matrixType == null || matrixType.isEmpty()) {
            return "pmatrix"; // Default: parentheses
        }
        
        switch (matrixType.toLowerCase()) {
            case "round":
            case "(":
                return "pmatrix";
            case "square":
            case "[":
                return "bmatrix";
            case "curly":
            case "{":
                return "Bmatrix";
            case "vertical":
            case "|":
                return "vmatrix";
            case "double":
            case "‖":
                return "Vmatrix";
            default:
                return "pmatrix";
        }
    }
}


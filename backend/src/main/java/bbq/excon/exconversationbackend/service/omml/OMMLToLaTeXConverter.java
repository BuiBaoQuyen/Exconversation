package bbq.excon.exconversationbackend.service.omml;

import java.util.List;
import java.util.ArrayList;

/**
 * Convert OMMLNode tree sang LaTeX
 * 
 * Sử dụng đệ quy để xử lý nested structures:
 * - Fractions trong fractions
 * - Radicals trong fractions
 * - Matrices trong fractions
 * - Operators với limits
 * - Delimiters động
 * 
 * Context-aware conversion: Group base với superscripts/subscripts
 * 
 * @author AI Assistant
 */
public class OMMLToLaTeXConverter {
    
    /**
     * Convert OMMLNode tree sang LaTeX string
     * 
     * @param root Root node của OMML tree
     * @return LaTeX string
     */
    public static String convert(OMMLNode root) {
        if (root == null) {
            return "";
        }
        
        try {
            // Step 1: Pre-process tree để group base với sup/sub
            preprocessTree(root);
            
            // Step 2: Convert với context awareness
            String latex = convertNode(root);
            
            // Step 3: Post-process LaTeX output
            latex = postProcessLaTeX(latex);
            
            return latex.trim();
        } catch (Exception e) {
            System.err.println("Error converting OMML to LaTeX: " + e.getMessage());
            e.printStackTrace();
            // Fallback: return text content
            return root.getAllTextContent();
        }
    }
    
    /**
     * Pre-process OMML tree để group base với superscripts/subscripts
     * Trong OMML, sup/sub có thể là siblings của base, cần group lại
     */
    private static void preprocessTree(OMMLNode node) {
        if (node == null) {
            return;
        }
        
        // Process children để group base với sup/sub
        List<OMMLNode> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            return;
        }
        
        // Group base với sup/sub trong children
        groupBaseWithSupSub(children);
        
        // Recursively process all children
        for (OMMLNode child : children) {
            preprocessTree(child);
        }
    }
    
    /**
     * Group base elements với superscripts/subscripts
     * Strategy: Tìm sup/sub và tìm base ở trước đó
     */
    private static void groupBaseWithSupSub(List<OMMLNode> children) {
        if (children == null || children.isEmpty()) {
            return;
        }
        
        for (int i = 0; i < children.size(); i++) {
            OMMLNode child = children.get(i);
            String localName = getLocalName(child.getTagName());
            
            // Check if this is a superscript or subscript
            if (isSuperscript(localName) || isSubscript(localName)) {
                // Find base (previous sibling that can be a base)
                OMMLNode base = findBaseForSupSub(children, i);
                
                if (base != null) {
                    // Mark base as having sup/sub
                    // We'll handle this during conversion
                    child.setAttribute("_hasBase", "true");
                    child.setAttribute("_baseIndex", String.valueOf(children.indexOf(base)));
                }
            }
        }
    }
    
    /**
     * Find base element for superscript/subscript
     * Base can be: text node, run, or any element that can have sup/sub
     */
    private static OMMLNode findBaseForSupSub(List<OMMLNode> children, int supSubIndex) {
        if (supSubIndex <= 0) {
            return null;
        }
        
        // Look backwards for a base
        for (int i = supSubIndex - 1; i >= 0; i--) {
            OMMLNode candidate = children.get(i);
            String localName = getLocalName(candidate.getTagName());
            
            // Skip if it's already a sup/sub or certain elements
            if (isSuperscript(localName) || isSubscript(localName) || 
                "f".equals(localName) || "rad".equals(localName) || 
                "m".equals(localName) || "nary".equals(localName)) {
                continue;
            }
            
            // Found a potential base
            return candidate;
        }
        
        return null;
    }
    
    /**
     * Check if tag is a superscript
     */
    private static boolean isSuperscript(String localName) {
        return "sup".equals(localName) || "sSup".equals(localName);
    }
    
    /**
     * Check if tag is a subscript
     */
    private static boolean isSubscript(String localName) {
        return "sub".equals(localName) || "sSub".equals(localName);
    }
    
    /**
     * Convert một node sang LaTeX (đệ quy)
     * 
     * @param node OMMLNode cần convert
     * @return LaTeX string
     */
    private static String convertNode(OMMLNode node) {
        if (node == null) {
            return "";
        }
        
        String tagName = node.getTagName();
        String localName = getLocalName(tagName);
        
        // Handle text nodes
        if (node.isTextNode()) {
            String text = OMMLNormalizer.normalizeText(node.getTextContent());
            return convertTextToLaTeX(text);
        }
        
        // Handle different OMML elements
        switch (localName) {
            case "oMath":
                // Root element: convert all children
                return convertChildren(node);
                
            case "r":
                // Run: convert children with context awareness
                return convertRunWithContext(node);
                
            case "t":
                // Text element
                String text = node.getAllTextContent();
                return convertTextToLaTeX(OMMLNormalizer.normalizeText(text));
                
            case "f":
                // Fraction: \frac{numerator}{denominator}
                return convertFraction(node);
                
            case "rad":
                // Radical: \sqrt[degree]{expression}
                return convertRadical(node);
                
            case "sup":
            case "sSup":
                // Superscript: base^{superscript} - handled with context
                return convertSuperscriptWithContext(node);
                
            case "sub":
            case "sSub":
                // Subscript: base_{subscript} - handled with context
                return convertSubscriptWithContext(node);
                
            case "m":
                // Matrix
                return convertMatrix(node);
                
            case "nary":
                // N-ary operator (∑, ∏, ∫) với limits
                return convertNaryOperator(node);
                
            case "d":
                // Delimiter (parentheses, brackets, etc.)
                return convertDelimiter(node);
                
            case "groupChr":
                // Group character (large brackets)
                return convertGroupCharacter(node);
                
            case "acc":
                // Accent
                return convertAccent(node);
                
            case "bar":
                // Bar/overline
                return convertBar(node);
                
            case "eqArr":
                // Equation array
                return convertEquationArray(node);
                
            case "phant":
                // Phantom (invisible element)
                return convertPhantom(node);
                
            case "box":
                // Box
                return convertBox(node);
                
            case "borderBox":
                // Border box
                return convertBorderBox(node);
                
            default:
                // Unknown element: convert children
                return convertChildren(node);
        }
    }
    
    /**
     * Convert children nodes (đệ quy)
     */
    private static String convertChildren(OMMLNode node) {
        if (node == null) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        List<OMMLNode> children = node.getChildren();
        
        if (children == null || children.isEmpty()) {
            return "";
        }
        
        for (OMMLNode child : children) {
            if (child != null) {
                result.append(convertNode(child));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Convert fraction: \frac{numerator}{denominator}
     */
    private static String convertFraction(OMMLNode fractionNode) {
        if (fractionNode == null) {
            return "";
        }
        
        OMMLNode numNode = fractionNode.findFirstChildByTag("m:num");
        OMMLNode denNode = fractionNode.findFirstChildByTag("m:den");
        
        String numerator = numNode != null ? convertNode(numNode).trim() : "";
        String denominator = denNode != null ? convertNode(denNode).trim() : "";
        
        // Nếu cả numerator và denominator đều rỗng, return empty
        if (numerator.isEmpty() && denominator.isEmpty()) {
            return "";
        }
        
        // Nếu một trong hai rỗng, dùng space để tránh lỗi LaTeX
        if (numerator.isEmpty()) {
            numerator = " ";
        }
        if (denominator.isEmpty()) {
            denominator = " ";
        }
        
        return "\\frac{" + numerator + "}{" + denominator + "}";
    }
    
    /**
     * Convert radical: \sqrt[degree]{expression}
     */
    private static String convertRadical(OMMLNode radicalNode) {
        if (radicalNode == null) {
            return "";
        }
        
        OMMLNode degNode = radicalNode.findFirstChildByTag("m:deg");
        OMMLNode exprNode = radicalNode.findFirstChildByTag("m:e");
        
        String expression = exprNode != null ? convertNode(exprNode).trim() : "";
        if (expression.isEmpty()) {
            return "";
        }
        
        if (degNode != null) {
            // nth root: \sqrt[n]{expr}
            String degree = convertNode(degNode).trim();
            if (!degree.isEmpty()) {
                return "\\sqrt[" + degree + "]{" + expression + "}";
            }
        }
        
        // Square root: \sqrt{expr}
        return "\\sqrt{" + expression + "}";
    }
    
    /**
     * Convert run with context awareness (handle sup/sub in siblings)
     */
    private static String convertRunWithContext(OMMLNode runNode) {
        if (runNode == null) {
            return "";
        }
        
        OMMLNode parent = runNode.getParent();
        if (parent == null) {
            return convertChildren(runNode);
        }
        
        List<OMMLNode> siblings = parent.getChildren();
        int runIndex = siblings.indexOf(runNode);
        
        if (runIndex < 0) {
            return convertChildren(runNode);
        }
        
        // Check if next siblings are sup/sub
        StringBuilder result = new StringBuilder();
        result.append(convertChildren(runNode));
        
        // Look ahead for sup/sub
        int i = runIndex + 1;
        while (i < siblings.size()) {
            OMMLNode sibling = siblings.get(i);
            String localName = getLocalName(sibling.getTagName());
            
            if (isSuperscript(localName)) {
                String sup = convertChildren(sibling);
                if (!sup.isEmpty()) {
                    result.append("^{").append(sup).append("}");
                }
                i++;
            } else if (isSubscript(localName)) {
                String sub = convertChildren(sibling);
                if (!sub.isEmpty()) {
                    result.append("_{").append(sub).append("}");
                }
                i++;
            } else {
                break; // Not sup/sub, stop looking
            }
        }
        
        return result.toString();
    }
    
    /**
     * Convert superscript with context awareness
     * If base was already processed, just return the superscript part
     */
    private static String convertSuperscriptWithContext(OMMLNode supNode) {
        // Check if base was already processed
        String hasBase = supNode.getAttribute("_hasBase");
        if ("true".equals(hasBase)) {
            // Base was already processed, just return superscript
            String superscript = convertChildren(supNode);
            if (superscript.isEmpty()) {
                return "";
            }
            return "^{" + superscript + "}";
        }
        
        // Try to find base in parent
        OMMLNode parent = supNode.getParent();
        if (parent != null) {
            List<OMMLNode> siblings = parent.getChildren();
            int supIndex = siblings.indexOf(supNode);
            
            if (supIndex > 0) {
                // Get base from previous sibling
                OMMLNode base = siblings.get(supIndex - 1);
                String baseLatex = convertNode(base);
                String superscript = convertChildren(supNode);
                
                if (!baseLatex.isEmpty() && !superscript.isEmpty()) {
                    return baseLatex + "^{" + superscript + "}";
                }
            }
        }
        
        // Fallback: just return superscript
        String superscript = convertChildren(supNode);
        if (superscript.isEmpty()) {
            return "";
        }
        return "^{" + superscript + "}";
    }
    
    /**
     * Convert subscript with context awareness
     */
    private static String convertSubscriptWithContext(OMMLNode subNode) {
        // Check if base was already processed
        String hasBase = subNode.getAttribute("_hasBase");
        if ("true".equals(hasBase)) {
            // Base was already processed, just return subscript
            String subscript = convertChildren(subNode);
            if (subscript.isEmpty()) {
                return "";
            }
            return "_{" + subscript + "}";
        }
        
        // Try to find base in parent
        OMMLNode parent = subNode.getParent();
        if (parent != null) {
            List<OMMLNode> siblings = parent.getChildren();
            int subIndex = siblings.indexOf(subNode);
            
            if (subIndex > 0) {
                // Get base from previous sibling
                OMMLNode base = siblings.get(subIndex - 1);
                String baseLatex = convertNode(base);
                String subscript = convertChildren(subNode);
                
                if (!baseLatex.isEmpty() && !subscript.isEmpty()) {
                    return baseLatex + "_{" + subscript + "}";
                }
            }
        }
        
        // Fallback: just return subscript
        String subscript = convertChildren(subNode);
        if (subscript.isEmpty()) {
            return "";
        }
        return "_{" + subscript + "}";
    }
    
    
    /**
     * Convert matrix
     */
    private static String convertMatrix(OMMLNode matrixNode) {
        List<OMMLNode> rows = matrixNode.findChildrenByTag("m:mr");
        
        if (rows.isEmpty()) {
            return "";
        }
        
        // Determine matrix type from properties
        String matrixType = "pmatrix"; // Default: parentheses
        OMMLNode mPrNode = matrixNode.findFirstChildByTag("m:mPr");
        if (mPrNode != null) {
            // Check for matrix type in properties
            // This is simplified - actual implementation would parse mPr
        }
        
        List<String> latexRows = new ArrayList<>();
        for (OMMLNode row : rows) {
            List<OMMLNode> cells = row.findChildrenByTag("m:e");
            List<String> cellContents = new ArrayList<>();
            
            for (OMMLNode cell : cells) {
                cellContents.add(convertNode(cell));
            }
            
            if (!cellContents.isEmpty()) {
                latexRows.add(String.join(" & ", cellContents));
            }
        }
        
        if (latexRows.isEmpty()) {
            return "";
        }
        
        String env = OMMLElementMapper.getMatrixEnvironment(matrixType);
        return "\\begin{" + env + "}\n" + 
               String.join(" \\\\\n", latexRows) + 
               "\n\\end{" + env + "}";
    }
    
    /**
     * Convert n-ary operator (∑, ∏, ∫) với limits
     */
    private static String convertNaryOperator(OMMLNode naryNode) {
        // Get operator character
        OMMLNode chrNode = naryNode.findFirstChildByTag("m:chr");
        String operator = "";
        if (chrNode != null) {
            operator = chrNode.getAllTextContent();
        }
        
        // Get limits
        OMMLNode subNode = naryNode.findFirstChildByTag("m:sub");
        OMMLNode supNode = naryNode.findFirstChildByTag("m:sup");
        OMMLNode exprNode = naryNode.findFirstChildByTag("m:e");
        
        String lowerLimit = subNode != null ? convertNode(subNode) : "";
        String upperLimit = supNode != null ? convertNode(supNode) : "";
        String expression = exprNode != null ? convertNode(exprNode) : "";
        
        // Convert operator to LaTeX
        String latexOp = OMMLElementMapper.getLaTeXForOperator(operator);
        if (latexOp.isEmpty()) {
            latexOp = operator;
        }
        
        // Build LaTeX
        StringBuilder result = new StringBuilder();
        
        if (!lowerLimit.isEmpty() || !upperLimit.isEmpty()) {
            // With limits: \sum_{lower}^{upper}
            result.append(latexOp);
            if (!lowerLimit.isEmpty()) {
                result.append("_{").append(lowerLimit).append("}");
            }
            if (!upperLimit.isEmpty()) {
                result.append("^{").append(upperLimit).append("}");
            }
        } else {
            // Without limits: just operator
            result.append(latexOp);
        }
        
        if (!expression.isEmpty()) {
            result.append(expression);
        }
        
        return result.toString();
    }
    
    /**
     * Convert delimiter (parentheses, brackets, etc.) with validation
     */
    private static String convertDelimiter(OMMLNode delimiterNode) {
        if (delimiterNode == null) {
            return "";
        }
        
        OMMLNode begChrNode = delimiterNode.findFirstChildByTag("m:begChr");
        OMMLNode endChrNode = delimiterNode.findFirstChildByTag("m:endChr");
        OMMLNode sepChrNode = delimiterNode.findFirstChildByTag("m:sepChr");
        OMMLNode exprNode = delimiterNode.findFirstChildByTag("m:e");
        
        String beginChar = begChrNode != null ? begChrNode.getAllTextContent().trim() : "(";
        String endChar = endChrNode != null ? endChrNode.getAllTextContent().trim() : ")";
        String separator = sepChrNode != null ? sepChrNode.getAllTextContent().trim() : "";
        
        // Validate delimiter matching
        if (endChar.isEmpty() || !isMatchingDelimiter(beginChar, endChar)) {
            // Fix: get matching delimiter
            endChar = getMatchingDelimiter(beginChar);
        }
        
        String expression = exprNode != null ? convertNode(exprNode).trim() : "";
        
        String beginLaTeX = OMMLElementMapper.getDelimiterLaTeX(beginChar);
        String endLaTeX = OMMLElementMapper.getDelimiterLaTeX(endChar);
        
        if (expression.isEmpty()) {
            return beginLaTeX + endLaTeX;
        }
        
        // Handle separator (for equation arrays)
        if (!separator.isEmpty() && expression.contains(separator)) {
            // Split by separator and wrap each part
            String[] parts = expression.split(separator, -1);
            List<String> wrappedParts = new ArrayList<>();
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    wrappedParts.add(part.trim());
                }
            }
            if (!wrappedParts.isEmpty()) {
                expression = String.join(" & ", wrappedParts);
            }
        }
        
        return beginLaTeX + expression + endLaTeX;
    }
    
    /**
     * Check if two delimiters match
     */
    private static boolean isMatchingDelimiter(String begin, String end) {
        if (begin == null || end == null) {
            return false;
        }
        
        return getMatchingDelimiter(begin).equals(end);
    }
    
    /**
     * Get matching delimiter for a given delimiter
     */
    private static String getMatchingDelimiter(String delimiter) {
        if (delimiter == null || delimiter.isEmpty()) {
            return ")";
        }
        
        switch (delimiter) {
            case "(": return ")";
            case "[": return "]";
            case "{": return "}";
            case "|": return "|";
            case "‖": return "‖";
            case "⌊": return "⌋";
            case "⌈": return "⌉";
            case "⟨": return "⟩";
            case ")": return "(";
            case "]": return "[";
            case "}": return "{";
            case "⌋": return "⌊";
            case "⌉": return "⌈";
            case "⟩": return "⟨";
            default: return ")";
        }
    }
    
    /**
     * Convert group character (large brackets)
     */
    private static String convertGroupCharacter(OMMLNode groupChrNode) {
        OMMLNode chrNode = groupChrNode.findFirstChildByTag("m:chr");
        OMMLNode exprNode = groupChrNode.findFirstChildByTag("m:e");
        
        String character = chrNode != null ? chrNode.getAllTextContent() : "(";
        String expression = exprNode != null ? convertNode(exprNode) : "";
        
        String beginLaTeX = OMMLElementMapper.getDelimiterLaTeX(character);
        String endLaTeX = OMMLElementMapper.getDelimiterLaTeX(getMatchingDelimiter(character));
        
        return beginLaTeX + expression + endLaTeX;
    }
    
    /**
     * Convert accent
     */
    private static String convertAccent(OMMLNode accentNode) {
        OMMLNode chrNode = accentNode.findFirstChildByTag("m:chr");
        OMMLNode exprNode = accentNode.findFirstChildByTag("m:e");
        
        String accent = chrNode != null ? chrNode.getAllTextContent() : "";
        String expression = exprNode != null ? convertNode(exprNode) : "";
        
        if (accent.isEmpty() || expression.isEmpty()) {
            return expression;
        }
        
        // Map accent to LaTeX command
        String accentCmd = getAccentCommand(accent);
        if (accentCmd != null) {
            return accentCmd + "{" + expression + "}";
        }
        
        return expression;
    }
    
    /**
     * Get LaTeX accent command
     */
    private static String getAccentCommand(String accent) {
        switch (accent) {
            case "̂": return "\\hat";
            case "̌": return "\\check";
            case "̆": return "\\breve";
            case "̇": return "\\dot";
            case "̈": return "\\ddot";
            case "⃗": return "\\vec";
            case "̄": return "\\bar";
            case "̃": return "\\tilde";
            default: return null;
        }
    }
    
    /**
     * Convert bar/overline
     */
    private static String convertBar(OMMLNode barNode) {
        OMMLNode exprNode = barNode.findFirstChildByTag("m:e");
        String expression = exprNode != null ? convertNode(exprNode) : "";
        
        if (expression.isEmpty()) {
            return "";
        }
        
        return "\\overline{" + expression + "}";
    }
    
    /**
     * Convert equation array
     */
    private static String convertEquationArray(OMMLNode eqArrNode) {
        List<OMMLNode> rows = eqArrNode.findChildrenByTag("m:e");
        
        if (rows.isEmpty()) {
            return "";
        }
        
        List<String> latexRows = new ArrayList<>();
        for (OMMLNode row : rows) {
            latexRows.add(convertNode(row));
        }
        
        return "\\begin{align}\n" + 
               String.join(" \\\\\n", latexRows) + 
               "\n\\end{align}";
    }
    
    /**
     * Convert phantom (invisible element)
     */
    private static String convertPhantom(OMMLNode phantomNode) {
        String expression = convertChildren(phantomNode);
        if (expression.isEmpty()) {
            return "";
        }
        return "\\phantom{" + expression + "}";
    }
    
    /**
     * Convert box
     */
    private static String convertBox(OMMLNode boxNode) {
        return convertChildren(boxNode);
    }
    
    /**
     * Convert border box
     */
    private static String convertBorderBox(OMMLNode borderBoxNode) {
        String expression = convertChildren(borderBoxNode);
        if (expression.isEmpty()) {
            return "";
        }
        return "\\boxed{" + expression + "}";
    }
    
    /**
     * Convert text to LaTeX (handle special characters, functions, etc.)
     * Wraps text content in \text{} if it's not a math symbol
     */
    private static String convertTextToLaTeX(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Normalize text
        text = OMMLNormalizer.normalizeText(text);
        
        // Check if text is a math symbol or expression
        if (isMathSymbol(text)) {
            return convertMathSymbol(text);
        }
        
        // Check if text is a unit or word (needs wrapping)
        if (isTextContent(text)) {
            return "\\text{" + escapeLaTeXText(text) + "}";
        }
        
        // Otherwise, treat as math expression
        return convertMathExpression(text);
    }
    
    /**
     * Check if text is a math symbol (single character or known symbol)
     */
    private static boolean isMathSymbol(String text) {
        if (text == null || text.length() > 2) {
            return false;
        }
        
        // Single character math symbols
        String mathSymbols = "∞→∈∀∃∄≤≥≠≈±∓×÷·αβγδεθλμπρστφω";
        if (text.length() == 1 && mathSymbols.contains(text)) {
            return true;
        }
        
        // Known functions
        if (OMMLElementMapper.isKnownFunction(text.toLowerCase())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if text is regular text content (units, words, etc.)
     */
    private static boolean isTextContent(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // Check for units (km, h, m, s, etc.)
        if (text.matches("^[a-zA-Z]+$") && text.length() <= 5) {
            String lower = text.toLowerCase();
            if (lower.matches("(km|h|m|s|kg|g|cm|mm|ml|l|km/h|m/s|kg/m|g/cm)")) {
                return true;
            }
        }
        
        // Check for words (contains letters but not just math)
        if (text.matches(".*[a-zA-Z]{2,}.*") && !text.matches("^[a-zA-Z]$")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Convert math symbol to LaTeX
     */
    private static String convertMathSymbol(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Convert common patterns
        text = text.replace("∞", "\\infty");
        text = text.replace("→", "\\to");
        text = text.replace("∈", "\\in");
        text = text.replace("∀", "\\forall");
        text = text.replace("∃", "\\exists");
        text = text.replace("∄", "\\nexists");
        text = text.replace("≤", "\\leq");
        text = text.replace("≥", "\\geq");
        text = text.replace("≠", "\\neq");
        text = text.replace("≈", "\\approx");
        text = text.replace("±", "\\pm");
        text = text.replace("∓", "\\mp");
        text = text.replace("×", "\\times");
        text = text.replace("÷", "\\div");
        text = text.replace("·", "\\cdot");
        text = text.replace("α", "\\alpha");
        text = text.replace("β", "\\beta");
        text = text.replace("γ", "\\gamma");
        text = text.replace("δ", "\\delta");
        text = text.replace("ε", "\\epsilon");
        text = text.replace("θ", "\\theta");
        text = text.replace("λ", "\\lambda");
        text = text.replace("μ", "\\mu");
        text = text.replace("π", "\\pi");
        text = text.replace("ρ", "\\rho");
        text = text.replace("σ", "\\sigma");
        text = text.replace("τ", "\\tau");
        text = text.replace("φ", "\\phi");
        text = text.replace("ω", "\\omega");
        
        // Convert function names
        String lowerText = text.toLowerCase();
        if (OMMLElementMapper.isKnownFunction(lowerText)) {
            return OMMLElementMapper.getLaTeXForFunction(lowerText);
        }
        
        return text;
    }
    
    /**
     * Convert math expression (may contain variables, operators, etc.)
     */
    private static String convertMathExpression(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Convert common patterns
        text = text.replace("∞", "\\infty");
        text = text.replace("→", "\\to");
        text = text.replace("∈", "\\in");
        text = text.replace("≤", "\\leq");
        text = text.replace("≥", "\\geq");
        text = text.replace("≠", "\\neq");
        text = text.replace("≈", "\\approx");
        text = text.replace("±", "\\pm");
        text = text.replace("∓", "\\mp");
        text = text.replace("×", "\\times");
        text = text.replace("÷", "\\div");
        text = text.replace("·", "\\cdot");
        
        // Convert "00" to "\\infty" in context of limits
        text = text.replaceAll("([-+]?)00", "$1\\infty");
        
        // Convert limit expressions: lim x → ∞
        text = convertLimitExpression(text);
        
        return text;
    }
    
    /**
     * Convert limit expression to LaTeX format
     */
    private static String convertLimitExpression(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // Pattern: lim x → ∞ or lim_{x→-∞}
        java.util.regex.Pattern limitPattern = java.util.regex.Pattern.compile(
            "lim\\s*([a-zA-Z]+)\\s*→\\s*([-+]?∞|[-+]?\\d+|[-+]?00)"
        );
        java.util.regex.Matcher matcher = limitPattern.matcher(text);
        
        if (matcher.find()) {
            String var = matcher.group(1);
            String target = matcher.group(2).replace("∞", "\\infty").replace("00", "\\infty");
            if (target.startsWith("+") && !target.startsWith("+\\")) {
                target = target.substring(1);
            }
            return text.replace(matcher.group(0), "\\lim_{" + var + " \\to " + target + "}");
        }
        
        // Pattern: limx (without space)
        text = text.replaceAll("lim([a-zA-Z]+)", "\\lim_{$1}");
        
        return text;
    }
    
    /**
     * Escape text for LaTeX \text{}
     */
    private static String escapeLaTeXText(String text) {
        if (text == null) {
            return "";
        }
        
        return text
            .replace("\\", "\\textbackslash{}")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("$", "\\$")
            .replace("&", "\\&")
            .replace("#", "\\#")
            .replace("^", "\\textasciicircum{}")
            .replace("_", "\\_")
            .replace("%", "\\%");
    }
    
    /**
     * Get local name from tag (remove namespace prefix)
     */
    private static String getLocalName(String tagName) {
        if (tagName == null) {
            return "";
        }
        if (tagName.contains(":")) {
            return tagName.substring(tagName.indexOf(":") + 1);
        }
        return tagName;
    }
    
    /**
     * Post-process LaTeX output để fix common errors
     * - Fix unmatched braces
     * - Fix unmatched delimiters
     * - Clean up spacing
     * - Fix common syntax errors
     */
    private static String postProcessLaTeX(String latex) {
        if (latex == null || latex.isEmpty()) {
            return latex;
        }
        
        // Step 1: Fix unmatched braces
        latex = fixUnmatchedBraces(latex);
        
        // Step 2: Fix unmatched delimiters
        latex = fixUnmatchedDelimiters(latex);
        
        // Step 3: Fix fractions with missing arguments
        latex = fixFractions(latex);
        
        // Step 4: Clean up spacing
        latex = cleanUpSpacing(latex);
        
        // Step 5: Fix common syntax errors
        latex = fixCommonSyntaxErrors(latex);
        
        return latex;
    }
    
    /**
     * Fix unmatched braces in LaTeX
     */
    private static String fixUnmatchedBraces(String latex) {
        if (latex == null || latex.isEmpty()) {
            return latex;
        }
        
        int openBraces = 0;
        int closeBraces = 0;
        
        // Count braces (but skip escaped braces and braces in \text{})
        boolean inText = false;
        for (int i = 0; i < latex.length(); i++) {
            char c = latex.charAt(i);
            
            if (i > 0 && latex.charAt(i - 1) == '\\') {
                // Escaped character, skip
                continue;
            }
            
            // Check if we're entering or leaving \text{}
            if (i < latex.length() - 5 && latex.substring(i, Math.min(i + 5, latex.length())).equals("\\text")) {
                inText = true;
                i += 4;
                continue;
            }
            
            if (inText && c == '}') {
                // Check if this closes \text{}
                int textStart = latex.lastIndexOf("\\text{", i);
                if (textStart >= 0) {
                    inText = false;
                }
            }
            
            if (!inText) {
                if (c == '{') {
                    openBraces++;
                } else if (c == '}') {
                    closeBraces++;
                }
            }
        }
        
        // Add missing closing braces
        if (openBraces > closeBraces) {
            int missing = openBraces - closeBraces;
            for (int i = 0; i < missing; i++) {
                latex += "}";
            }
        }
        
        return latex;
    }
    
    /**
     * Fix unmatched delimiters (\left without \right, etc.)
     */
    private static String fixUnmatchedDelimiters(String latex) {
        if (latex == null || latex.isEmpty()) {
            return latex;
        }
        
        // Count \left and \right
        int leftCount = 0;
        int rightCount = 0;
        
        java.util.regex.Pattern leftPattern = java.util.regex.Pattern.compile("\\\\left[^a-zA-Z]");
        java.util.regex.Pattern rightPattern = java.util.regex.Pattern.compile("\\\\right[^a-zA-Z]");
        
        java.util.regex.Matcher leftMatcher = leftPattern.matcher(latex);
        while (leftMatcher.find()) {
            leftCount++;
        }
        
        java.util.regex.Matcher rightMatcher = rightPattern.matcher(latex);
        while (rightMatcher.find()) {
            rightCount++;
        }
        
        // Add missing \right)
        if (leftCount > rightCount) {
            int missing = leftCount - rightCount;
            for (int i = 0; i < missing; i++) {
                latex += "\\right)";
            }
        }
        
        // Remove extra \right (simplified - just remove from end)
        if (rightCount > leftCount) {
            int extra = rightCount - leftCount;
            for (int i = 0; i < extra; i++) {
                int lastRight = latex.lastIndexOf("\\right");
                if (lastRight >= 0) {
                    // Find the end of \right...)
                    int end = lastRight;
                    while (end < latex.length() && latex.charAt(end) != ')') {
                        end++;
                    }
                    if (end < latex.length()) {
                        latex = latex.substring(0, lastRight) + latex.substring(end + 1);
                    }
                }
            }
        }
        
        return latex;
    }
    
    /**
     * Fix fractions with missing arguments
     */
    private static String fixFractions(String latex) {
        if (latex == null || latex.isEmpty()) {
            return latex;
        }
        
        // Fix \frac{^{...} (fraction with superscript in wrong place)
        latex = latex.replaceAll("\\\\frac\\{([^}]*)\\^\\{", "\\frac{$1}{");
        
        // Fix \frac{} (empty fraction)
        latex = latex.replaceAll("\\\\frac\\{\\}\\{([^}]*)\\}", "\\frac{1}{$1}");
        latex = latex.replaceAll("\\\\frac\\{([^}]*)\\}\\{\\}", "\\frac{$1}{1}");
        latex = latex.replaceAll("\\\\frac\\{\\}\\{\\}", "\\frac{1}{1}");
        
        // Fix Missing argument for \frac
        latex = latex.replaceAll("Missing argument for \\\\frac", "");
        
        return latex;
    }
    
    /**
     * Clean up spacing in LaTeX
     */
    private static String cleanUpSpacing(String latex) {
        if (latex == null || latex.isEmpty()) {
            return latex;
        }
        
        // Remove multiple spaces
        latex = latex.replaceAll("\\s+", " ");
        
        // Remove spaces before punctuation in math
        latex = latex.replaceAll("\\s+([,.;:!?])", "$1");
        
        // Remove spaces after opening braces
        latex = latex.replaceAll("\\{\\s+", "{");
        
        // Remove spaces before closing braces
        latex = latex.replaceAll("\\s+\\}", "}");
        
        // Clean up around operators
        latex = latex.replaceAll("\\s*=\\s*", "=");
        latex = latex.replaceAll("\\s*\\+\\s*", "+");
        latex = latex.replaceAll("\\s*-\\s*", "-");
        latex = latex.replaceAll("\\s*\\*\\s*", "*");
        
        return latex.trim();
    }
    
    /**
     * Fix common syntax errors
     * Strategy: Fix errors BEFORE removing error messages
     */
    private static String fixCommonSyntaxErrors(String latex) {
        if (latex == null || latex.isEmpty()) {
            return latex;
        }
        
        // Step 1: Fix superscripts/subscripts với missing closing braces
        // Pattern: ^{x^{2}Extra close brace → ^{x^{2}}
        latex = fixNestedSupSub(latex);
        
        // Step 2: Fix unmatched delimiters trong expressions
        // Pattern: \left(a;bMissing → \left(a;b\right)
        latex = fixUnmatchedDelimitersInExpressions(latex);
        
        // Step 3: Fix fractions với missing arguments
        // Pattern: \frac{1} =Missing → \frac{1}{?}
        latex = fixIncompleteFractions(latex);
        
        // Step 4: Fix incomplete text blocks
        // Pattern: \text{=1(km/} → \text{=1(km/)}
        latex = fixIncompleteTextBlocks(latex);
        
        // Step 5: Fix double exponents
        // Pattern: ^{x^{2}Extra → ^{x^{2}}
        latex = fixDoubleExponents(latex);
        
        // Step 6: Remove error messages (sau khi đã fix)
        latex = latex.replaceAll("Extra close brace or missing open brace", "");
        latex = latex.replaceAll("Missing or unrecognized delimiter for", "");
        latex = latex.replaceAll("Extra \\\\left or missing \\\\right", "");
        latex = latex.replaceAll("Missing \\\\left or extra \\\\right", "");
        latex = latex.replaceAll("Missing argument for \\\\frac", "");
        latex = latex.replaceAll("Double exponent: use braces to clarify", "");
        
        // Step 7: Fix \limx (should be \lim_{x})
        latex = latex.replaceAll("\\\\lim([a-zA-Z]+)(?![_^])", "\\\\lim_{$1}");
        
        // Step 8: Clean up multiple consecutive spaces
        latex = latex.replaceAll("\\s+\\s+", " ");
        
        return latex.trim();
    }
    
    /**
     * Fix nested superscripts/subscripts với missing closing braces
     * Pattern: ^{x^{2}Extra close brace → ^{x^{2}}
     */
    private static String fixNestedSupSub(String latex) {
        if (latex == null || latex.isEmpty()) {
            return latex;
        }
        
        // Fix ^{x^{2}Extra → ^{x^{2}}
        latex = latex.replaceAll("\\^\\{([^}]*?)\\^\\{([^}]*?)\\}([^}]*?)Extra close brace", "^{$1^{$2}$3}");
        latex = latex.replaceAll("\\^\\{([^}]*?)\\^\\{([^}]*?)\\}([^}]*?)Missing", "^{$1^{$2}$3}");
        
        // Fix _{x_{1}Extra → _{x_{1}}
        latex = latex.replaceAll("_\\{([^}]*?)_\\{([^}]*?)\\}([^}]*?)Extra close brace", "_{$1_{$2}$3}");
        latex = latex.replaceAll("_\\{([^}]*?)_\\{([^}]*?)\\}([^}]*?)Missing", "_{$1_{$2}$3}");
        
        // Fix simple cases: ^{xExtra → ^{x}
        latex = latex.replaceAll("\\^\\{([^}]+)Extra close brace", "^{$1}");
        latex = latex.replaceAll("_\\{([^}]+)Extra close brace", "_{$1}");
        
        return latex;
    }
    
    /**
     * Fix unmatched delimiters trong expressions
     * Pattern: \left(a;bMissing → \left(a;b\right)
     */
    private static String fixUnmatchedDelimitersInExpressions(String latex) {
        if (latex == null || latex.isEmpty()) {
            return latex;
        }
        
        // Fix \left(...Missing → \left(...\right)
        latex = latex.replaceAll("\\\\left\\(([^)]*?)Missing or unrecognized delimiter for", "\\\\left($1\\\\right)");
        latex = latex.replaceAll("\\\\left\\(([^)]*?)Missing \\\\left or extra", "\\\\left($1\\\\right)");
        latex = latex.replaceAll("\\\\left\\(([^)]*?)Extra \\\\left or missing", "\\\\left($1\\\\right)");
        
        // Fix \left[...Missing → \left[...\right]
        latex = latex.replaceAll("\\\\left\\[([^\\]]*?)Missing or unrecognized delimiter for", "\\\\left[$1\\\\right]");
        latex = latex.replaceAll("\\\\left\\[([^\\]]*?)Missing \\\\left or extra", "\\\\left[$1\\\\right]");
        latex = latex.replaceAll("\\\\left\\[([^\\]]*?)Extra \\\\left or missing", "\\\\left[$1\\\\right]");
        
        // Fix \left{...Missing → \left{...\right}
        latex = latex.replaceAll("\\\\left\\{([^}]*?)Missing or unrecognized delimiter for", "\\\\left{$1\\\\right}");
        latex = latex.replaceAll("\\\\left\\{([^}]*?)Missing \\\\left or extra", "\\\\left{$1\\\\right}");
        latex = latex.replaceAll("\\\\left\\{([^}]*?)Extra \\\\left or missing", "\\\\left{$1\\\\right}");
        
        // Fix standalone delimiters: (a;bMissing → (a;b)
        latex = latex.replaceAll("\\(([^)]*?);([^)]*?)Missing or unrecognized delimiter for", "($1;$2)");
        latex = latex.replaceAll("\\[([^\\]]*?);([^\\]]*?)Missing or unrecognized delimiter for", "[$1;$2]");
        
        return latex;
    }
    
    /**
     * Fix fractions với missing arguments
     * Pattern: \frac{1} =Missing → \frac{1}{?}
     */
    private static String fixIncompleteFractions(String latex) {
        if (latex == null || latex.isEmpty()) {
            return latex;
        }
        
        // Fix \frac{num} =Missing → \frac{num}{?}
        latex = latex.replaceAll("\\\\frac\\{([^}]+)\\}\\s*=\\s*Missing argument for", "\\\\frac{$1}{?}");
        latex = latex.replaceAll("\\\\frac\\{([^}]+)\\}\\s*Missing argument for", "\\\\frac{$1}{?}");
        
        // Fix \frac{num} (không có denominator)
        latex = latex.replaceAll("\\\\frac\\{([^}]+)\\}(?![{])", "\\\\frac{$1}{?}");
        
        // Fix \frac{}{den} → \frac{?}{den}
        latex = latex.replaceAll("\\\\frac\\{\\}\\{([^}]+)\\}", "\\\\frac{?}{$1}");
        
        // Fix \frac{} → \frac{?}{?}
        latex = latex.replaceAll("\\\\frac\\{\\}\\{\\}", "\\\\frac{?}{?}");
        
        return latex;
    }
    
    /**
     * Fix incomplete text blocks
     * Pattern: \text{=1(km/} → \text{=1(km/)}
     */
    private static String fixIncompleteTextBlocks(String latex) {
        if (latex == null || latex.isEmpty()) {
            return latex;
        }
        
        // Fix \text{...} với missing closing brace
        // Strategy: Tìm \text{... và đảm bảo có closing brace
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "\\\\text\\{([^}]*?)(?=\\s|$|\\\\|\\^|_|\\[|\\]|\\(|\\)|\\{|\\})"
        );
        
        java.util.regex.Matcher matcher = pattern.matcher(latex);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String content = matcher.group(1);
            // Nếu không có closing brace, thêm vào
            if (!matcher.group(0).endsWith("}")) {
                matcher.appendReplacement(result, "\\\\text{" + content + "}");
            } else {
                matcher.appendReplacement(result, matcher.group(0));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * Fix double exponents
     * Pattern: ^{x^{2}Extra → ^{x^{2}}
     */
    private static String fixDoubleExponents(String latex) {
        if (latex == null || latex.isEmpty()) {
            return latex;
        }
        
        // Fix ^{x^{2}Extra → ^{x^{2}}
        latex = latex.replaceAll("\\^\\{([^}]*?)\\^\\{([^}]*?)\\}([^}]*?)Extra close brace", "^{$1^{$2}$3}");
        latex = latex.replaceAll("\\^\\{([^}]*?)\\^\\{([^}]*?)\\}([^}]*?)Double exponent", "^{$1^{$2}$3}");
        
        // Fix ^{x^{2} (missing closing brace)
        latex = latex.replaceAll("\\^\\{([^}]*?)\\^\\{([^}]*?)\\}([^}]*?)(?=\\s|$|\\\\|\\^|_|\\[|\\]|\\(|\\)|\\{|\\})", "^{$1^{$2}$3}");
        
        return latex;
    }
}


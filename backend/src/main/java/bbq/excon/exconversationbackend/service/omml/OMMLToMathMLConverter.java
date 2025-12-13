package bbq.excon.exconversationbackend.service.omml;

import java.util.List;

/**
 * Convert OMMLNode tree sang MathML
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
 * Tương tự OMMLToLaTeXConverter nhưng output là MathML XML
 */
public class OMMLToMathMLConverter {
    
    /**
     * Convert OMMLNode tree sang MathML string
     * 
     * @param root Root node của OMML tree
     * @return MathML string
     */
    public static String convert(OMMLNode root) {
        if (root == null) {
            return "";
        }
        
        try {
            // Step 1: Pre-process tree để group base với sup/sub
            preprocessTree(root);
            
            // Step 2: Convert với context awareness
            String mathml = convertNode(root);
            
            // Step 3: Post-process MathML output
            mathml = postProcessMathML(mathml);
            
            return mathml.trim();
        } catch (Exception e) {
            System.err.println("Error converting OMML to MathML: " + e.getMessage());
            e.printStackTrace();
            // Fallback: return text content wrapped in math tag
            String text = root.getAllTextContent();
            if (text != null && !text.trim().isEmpty()) {
                return "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><mi>" + 
                       escapeMathMLText(text) + "</mi></math>";
            }
            return "";
        }
    }
    
    /**
     * Pre-process OMML tree để group base với superscripts/subscripts
     */
    private static void preprocessTree(OMMLNode node) {
        if (node == null) {
            return;
        }
        
        List<OMMLNode> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            return;
        }
        
        groupBaseWithSupSub(children);
        
        for (OMMLNode child : children) {
            preprocessTree(child);
        }
    }
    
    /**
     * Group base elements với superscripts/subscripts
     */
    private static void groupBaseWithSupSub(List<OMMLNode> children) {
        if (children == null || children.isEmpty()) {
            return;
        }
        
        for (int i = 0; i < children.size(); i++) {
            OMMLNode child = children.get(i);
            String localName = getLocalName(child.getTagName());
            
            if (isSuperscript(localName) || isSubscript(localName)) {
                OMMLNode base = findBaseForSupSub(children, i);
                
                if (base != null) {
                    child.setAttribute("_hasBase", "true");
                    child.setAttribute("_baseIndex", String.valueOf(children.indexOf(base)));
                }
            }
        }
    }
    
    private static OMMLNode findBaseForSupSub(List<OMMLNode> children, int supSubIndex) {
        if (supSubIndex <= 0) {
            return null;
        }
        
        for (int i = supSubIndex - 1; i >= 0; i--) {
            OMMLNode candidate = children.get(i);
            String localName = getLocalName(candidate.getTagName());
            
            if (isSuperscript(localName) || isSubscript(localName) || 
                "f".equals(localName) || "rad".equals(localName) || 
                "m".equals(localName) || "nary".equals(localName)) {
                continue;
            }
            
            return candidate;
        }
        
        return null;
    }
    
    private static boolean isSuperscript(String localName) {
        return "sup".equals(localName) || "sSup".equals(localName);
    }
    
    private static boolean isSubscript(String localName) {
        return "sub".equals(localName) || "sSub".equals(localName);
    }
    
    /**
     * Convert một node sang MathML (đệ quy)
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
            return convertTextToMathML(text);
        }
        
        // Handle different OMML elements
        switch (localName) {
            case "oMath":
                // Root element: wrap in <math> tag
                String children = convertChildren(node);
                return "<math xmlns=\"http://www.w3.org/1998/Math/MathML\">" + children + "</math>";
                
            case "r":
                return convertRun(node);
                
            case "t":
                String text = node.getAllTextContent();
                return convertTextToMathML(OMMLNormalizer.normalizeText(text));
                
            case "f":
                return convertFraction(node);
                
            case "rad":
                return convertRadical(node);
                
            case "sup":
            case "sSup":
                return convertSuperscriptWithContext(node);
                
            case "sub":
            case "sSub":
                return convertSubscriptWithContext(node);
                
            case "m":
                return convertMatrix(node);
                
            case "nary":
                return convertNaryOperator(node);
                
            case "d":
                return convertDelimiter(node);
                
            case "acc":
                return convertAccent(node);
                
            case "bar":
                return convertBar(node);
                
            case "eqArr":
                return convertEquationArray(node);
                
            default:
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
     * Convert fraction: <mfrac><mn>numerator</mn><mn>denominator</mn></mfrac>
     */
    private static String convertFraction(OMMLNode fractionNode) {
        if (fractionNode == null) {
            return "";
        }
        
        OMMLNode numNode = fractionNode.findFirstChildByTag("m:num");
        OMMLNode denNode = fractionNode.findFirstChildByTag("m:den");
        
        String numerator = numNode != null ? convertNode(numNode).trim() : "";
        String denominator = denNode != null ? convertNode(denNode).trim() : "";
        
        if (numerator.isEmpty()) numerator = "<mi></mi>";
        if (denominator.isEmpty()) denominator = "<mi></mi>";
        
        return "<mfrac>" + numerator + denominator + "</mfrac>";
    }
    
    /**
     * Convert radical: <msqrt> hoặc <mroot>
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
            // nth root: <mroot>base index</mroot>
            // Note: mroot structure is <mroot><base><index></mroot>
            String degree = convertNode(degNode).trim();
            if (!degree.isEmpty()) {
                // Ensure degree is wrapped in proper MathML element if needed
                if (!degree.startsWith("<")) {
                    degree = "<mn>" + escapeMathMLText(degree) + "</mn>";
                }
                // mroot: first child is base (expression), second is index (degree)
                return "<mroot>" + expression + degree + "</mroot>";
            }
        }
        
        // Square root: <msqrt>expression</msqrt>
        return "<msqrt>" + expression + "</msqrt>";
    }
    
    /**
     * Convert run with context awareness
     * Note: sup/sub sẽ được xử lý riêng trong convertSuperscriptWithContext/convertSubscriptWithContext
     * Nên chỉ convert children của run, không wrap với sup/sub ở đây
     */
    private static String convertRun(OMMLNode runNode) {
        if (runNode == null) {
            return "";
        }
        
        // Chỉ convert children của run
        // Sup/sub sẽ được xử lý riêng khi gặp node sup/sub
        return convertChildren(runNode);
    }
    
    /**
     * Convert superscript with context awareness
     */
    private static String convertSuperscriptWithContext(OMMLNode supNode) {
        String hasBase = supNode.getAttribute("_hasBase");
        if ("true".equals(hasBase)) {
            String superscript = convertChildren(supNode);
            if (superscript.isEmpty()) {
                return "";
            }
            return superscript; // Base was already processed
        }
        
        OMMLNode parent = supNode.getParent();
        if (parent != null) {
            List<OMMLNode> siblings = parent.getChildren();
            int supIndex = siblings.indexOf(supNode);
            
            if (supIndex > 0) {
                OMMLNode base = siblings.get(supIndex - 1);
                String baseMathml = convertNode(base);
                String superscript = convertChildren(supNode);
                
                if (!baseMathml.isEmpty() && !superscript.isEmpty()) {
                    return "<msup>" + baseMathml + superscript + "</msup>";
                }
            }
        }
        
        String superscript = convertChildren(supNode);
        if (superscript.isEmpty()) {
            return "";
        }
        return "<msup><mi></mi>" + superscript + "</msup>";
    }
    
    /**
     * Convert subscript with context awareness
     */
    private static String convertSubscriptWithContext(OMMLNode subNode) {
        String hasBase = subNode.getAttribute("_hasBase");
        if ("true".equals(hasBase)) {
            String subscript = convertChildren(subNode);
            if (subscript.isEmpty()) {
                return "";
            }
            return subscript; // Base was already processed
        }
        
        OMMLNode parent = subNode.getParent();
        if (parent != null) {
            List<OMMLNode> siblings = parent.getChildren();
            int subIndex = siblings.indexOf(subNode);
            
            if (subIndex > 0) {
                OMMLNode base = siblings.get(subIndex - 1);
                String baseMathml = convertNode(base);
                String subscript = convertChildren(subNode);
                
                if (!baseMathml.isEmpty() && !subscript.isEmpty()) {
                    return "<msub>" + baseMathml + subscript + "</msub>";
                }
            }
        }
        
        String subscript = convertChildren(subNode);
        if (subscript.isEmpty()) {
            return "";
        }
        return "<msub><mi></mi>" + subscript + "</msub>";
    }
    
    /**
     * Convert matrix
     */
    private static String convertMatrix(OMMLNode matrixNode) {
        List<OMMLNode> rows = matrixNode.findChildrenByTag("m:mr");
        
        if (rows.isEmpty()) {
            return "";
        }
        
        StringBuilder mathml = new StringBuilder("<mtable>");
        
        for (OMMLNode row : rows) {
            List<OMMLNode> cells = row.findChildrenByTag("m:e");
            mathml.append("<mtr>");
            
            for (OMMLNode cell : cells) {
                mathml.append("<mtd>").append(convertNode(cell)).append("</mtd>");
            }
            
            mathml.append("</mtr>");
        }
        
        mathml.append("</mtable>");
        return mathml.toString();
    }
    
    /**
     * Convert n-ary operator (∑, ∏, ∫) với limits
     */
    private static String convertNaryOperator(OMMLNode naryNode) {
        OMMLNode chrNode = naryNode.findFirstChildByTag("m:chr");
        String operator = "";
        if (chrNode != null) {
            operator = chrNode.getAllTextContent();
        }
        
        OMMLNode subNode = naryNode.findFirstChildByTag("m:sub");
        OMMLNode supNode = naryNode.findFirstChildByTag("m:sup");
        OMMLNode exprNode = naryNode.findFirstChildByTag("m:e");
        
        String lowerLimit = subNode != null ? convertNode(subNode) : "";
        String upperLimit = supNode != null ? convertNode(supNode) : "";
        String expression = exprNode != null ? convertNode(exprNode) : "";
        
        // Convert operator to MathML
        String mathmlOp = convertOperatorToMathML(operator);
        
        StringBuilder result = new StringBuilder();
        
        if (!lowerLimit.isEmpty() || !upperLimit.isEmpty()) {
            // With limits: <munderover><mo>operator</mo><munder>lower</munder><mover>upper</mover></munderover>
            result.append("<munderover>");
            result.append("<mo>").append(escapeMathMLText(mathmlOp)).append("</mo>");
            if (!lowerLimit.isEmpty()) {
                result.append("<munder>").append(lowerLimit).append("</munder>");
            } else {
                result.append("<munder><mi></mi></munder>");
            }
            if (!upperLimit.isEmpty()) {
                result.append("<mover>").append(upperLimit).append("</mover>");
            } else {
                result.append("<mover><mi></mi></mover>");
            }
            result.append("</munderover>");
        } else {
            // Without limits: just operator
            result.append("<mo>").append(escapeMathMLText(mathmlOp)).append("</mo>");
        }
        
        if (!expression.isEmpty()) {
            result.append(expression);
        }
        
        return result.toString();
    }
    
    /**
     * Convert delimiter (parentheses, brackets, etc.)
     */
    private static String convertDelimiter(OMMLNode delimiterNode) {
        if (delimiterNode == null) {
            return "";
        }
        
        OMMLNode begChrNode = delimiterNode.findFirstChildByTag("m:begChr");
        OMMLNode endChrNode = delimiterNode.findFirstChildByTag("m:endChr");
        OMMLNode exprNode = delimiterNode.findFirstChildByTag("m:e");
        
        String beginChar = begChrNode != null ? begChrNode.getAllTextContent().trim() : "(";
        String endChar = endChrNode != null ? endChrNode.getAllTextContent().trim() : ")";
        
        String expression = exprNode != null ? convertNode(exprNode).trim() : "";
        
        // Use <mfenced> for delimiters
        String open = escapeMathMLText(beginChar);
        String close = escapeMathMLText(endChar);
        
        return "<mfenced open=\"" + open + "\" close=\"" + close + "\">" + 
               expression + "</mfenced>";
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
        
        // Use <mover> for accents
        return "<mover><mo>" + escapeMathMLText(accent) + "</mo>" + expression + "</mover>";
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
        
        return "<mover>" + expression + "<mo>¯</mo></mover>";
    }
    
    /**
     * Convert equation array
     */
    private static String convertEquationArray(OMMLNode eqArrNode) {
        List<OMMLNode> rows = eqArrNode.findChildrenByTag("m:e");
        
        if (rows.isEmpty()) {
            return "";
        }
        
        StringBuilder mathml = new StringBuilder("<mtable>");
        
        for (OMMLNode row : rows) {
            mathml.append("<mtr><mtd>").append(convertNode(row)).append("</mtd></mtr>");
        }
        
        mathml.append("</mtable>");
        return mathml.toString();
    }
    
    /**
     * Convert text to MathML (determine if identifier, number, or operator)
     * Xử lý text có thể chứa nhiều phần: variables, operators, numbers
     */
    private static String convertTextToMathML(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        text = OMMLNormalizer.normalizeText(text);
        
        // Nếu text chỉ là một số, operator, hoặc identifier đơn giản
        if (isNumber(text)) {
            return "<mn>" + escapeMathMLText(text) + "</mn>";
        } else if (isOperator(text)) {
            return "<mo>" + escapeMathMLText(text) + "</mo>";
        } else if (text.length() == 1 && text.matches("[a-zA-Z]")) {
            // Single letter identifier
            return "<mi>" + escapeMathMLText(text) + "</mi>";
        }
        
        // Text phức tạp: cần parse thành các phần
        // Ví dụ: "y=ax-4bx+c" -> <mi>y</mi><mo>=</mo><mi>a</mi><mi>x</mi><mo>-</mo>...
        return parseMathExpressionToMathML(text);
    }
    
    /**
     * Parse math expression text thành MathML elements
     * Ví dụ: "y=ax-4bx+c" -> <mi>y</mi><mo>=</mo><mi>a</mi><mi>x</mi><mo>-</mo><mn>4</mn>...
     */
    private static String parseMathExpressionToMathML(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        StringBuilder currentToken = new StringBuilder();
        boolean inNumber = false;
        boolean expectOperator = false; // Track if we just processed an operator (for negative numbers)
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            // Check if character is operator (but not minus at start of number)
            if (isOperatorChar(c) && !(c == '-' && (i == 0 || expectOperator || currentToken.length() == 0))) {
                // Flush current token if any
                if (currentToken.length() > 0) {
                    String token = currentToken.toString();
                    if (isNumber(token)) {
                        result.append("<mn>").append(escapeMathMLText(token)).append("</mn>");
                    } else {
                        // Identifier - split into individual characters
                        for (char ch : token.toCharArray()) {
                            result.append("<mi>").append(escapeMathMLText(String.valueOf(ch))).append("</mi>");
                        }
                    }
                    currentToken.setLength(0);
                    inNumber = false;
                }
                // Add operator
                result.append("<mo>").append(escapeMathMLText(String.valueOf(c))).append("</mo>");
                expectOperator = true;
            } else if (Character.isDigit(c) || c == '.' || (c == '-' && (i == 0 || expectOperator || currentToken.length() == 0))) {
                // Number part (including negative sign)
                if (!inNumber && currentToken.length() > 0) {
                    // Flush identifier token
                    String token = currentToken.toString();
                    for (char ch : token.toCharArray()) {
                        result.append("<mi>").append(escapeMathMLText(String.valueOf(ch))).append("</mi>");
                    }
                    currentToken.setLength(0);
                }
                currentToken.append(c);
                inNumber = true;
                expectOperator = false;
            } else if (Character.isLetter(c) || c == '∈' || c == 'R' || c == '∞') {
                // Identifier part
                if (inNumber && currentToken.length() > 0) {
                    // Flush number token
                    result.append("<mn>").append(escapeMathMLText(currentToken.toString())).append("</mn>");
                    currentToken.setLength(0);
                    inNumber = false;
                }
                currentToken.append(c);
                expectOperator = false;
            } else {
                // Other characters (punctuation, etc.)
                if (currentToken.length() > 0) {
                    String token = currentToken.toString();
                    if (isNumber(token)) {
                        result.append("<mn>").append(escapeMathMLText(token)).append("</mn>");
                    } else {
                        for (char ch : token.toCharArray()) {
                            result.append("<mi>").append(escapeMathMLText(String.valueOf(ch))).append("</mi>");
                        }
                    }
                    currentToken.setLength(0);
                    inNumber = false;
                }
                // Add as operator or text
                if (c == ',' || c == ';' || c == ':' || c == '(' || c == ')') {
                    result.append("<mo>").append(escapeMathMLText(String.valueOf(c))).append("</mo>");
                } else {
                    result.append("<mi>").append(escapeMathMLText(String.valueOf(c))).append("</mi>");
                }
                expectOperator = false;
            }
        }
        
        // Flush remaining token
        if (currentToken.length() > 0) {
            String token = currentToken.toString();
            if (isNumber(token)) {
                result.append("<mn>").append(escapeMathMLText(token)).append("</mn>");
            } else {
                for (char ch : token.toCharArray()) {
                    result.append("<mi>").append(escapeMathMLText(String.valueOf(ch))).append("</mi>");
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Check if character is an operator
     */
    private static boolean isOperatorChar(char c) {
        String operators = "+-*/=<>≤≥≠≈±×÷·→";
        return operators.indexOf(c) >= 0;
    }
    
    private static boolean isNumber(String text) {
        return text.matches("^-?\\d+(\\.\\d+)?$");
    }
    
    private static boolean isOperator(String text) {
        String operators = "+-*/=<>≤≥≠≈±×÷·∈→";
        return text.length() == 1 && operators.contains(text);
    }
    
    /**
     * Convert operator character to MathML representation
     */
    private static String convertOperatorToMathML(String operator) {
        if (operator == null || operator.isEmpty()) {
            return "";
        }
        
        // Map common operators
        switch (operator) {
            case "∑": return "∑";
            case "∏": return "∏";
            case "∫": return "∫";
            default: return operator;
        }
    }
    
    /**
     * Escape text for MathML
     */
    private static String escapeMathMLText(String text) {
        if (text == null) {
            return "";
        }
        
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
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
     * Post-process MathML output để fix common errors
     */
    private static String postProcessMathML(String mathml) {
        if (mathml == null || mathml.isEmpty()) {
            return mathml;
        }
        
        // Remove duplicate <math> tags
        mathml = mathml.replaceAll("</math>\\s*<math[^>]*>", "");
        
        // Clean up whitespace
        mathml = mathml.replaceAll(">\\s+<", "><");
        mathml = mathml.replaceAll("\\s+", " ");
        
        return mathml.trim();
    }
}


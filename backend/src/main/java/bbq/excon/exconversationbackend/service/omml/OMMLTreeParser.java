package bbq.excon.exconversationbackend.service.omml;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCursor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

import org.xml.sax.InputSource;

/**
 * Parse OMML XML thành tree structure (OMMLNode)
 * 
 * Sử dụng DOM parser để parse OMML XML một cách chính xác,
 * hỗ trợ nested structures và preserve order
 * 
 * @author AI Assistant
 */
public class OMMLTreeParser {
    
    private static final String OMML_PREFIX = "m";
    
    /**
     * Parse OMML XML string thành OMMLNode tree
     * 
     * @param ommlXml OMML XML string
     * @return Root OMMLNode của tree
     * @throws Exception Nếu parse fails
     */
    public static OMMLNode parse(String ommlXml) throws Exception {
        if (ommlXml == null || ommlXml.trim().isEmpty()) {
            throw new IllegalArgumentException("OMML XML cannot be null or empty");
        }
        
        // Normalize input trước khi parse
        String normalized = OMMLNormalizer.normalize(ommlXml);
        
        // Ensure XML is well-formed (wrap if needed)
        if (!normalized.trim().startsWith("<")) {
            throw new IllegalArgumentException("Invalid OMML XML: does not start with '<'");
        }
        
        try {
            // Parse XML using DOM
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(false);
            
            // Security: Disable external entity processing to prevent XXE attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(normalized)));
            
            // Find root element (m:oMath)
            Element rootElement = doc.getDocumentElement();
            if (rootElement == null) {
                throw new Exception("No root element found in OMML XML");
            }
            
            // Convert DOM tree to OMMLNode tree
            OMMLNode rootNode = convertDOMNodeToOMMLNode(rootElement);
            if (rootNode == null) {
                throw new Exception("Failed to convert DOM node to OMMLNode");
            }
            
            return rootNode;
            
        } catch (Exception e) {
            // Fallback: Try parsing with XmlObject (Apache XMLBeans)
            try {
                return parseWithXmlObject(normalized);
            } catch (Exception ex) {
                throw new Exception("Failed to parse OMML XML: " + e.getMessage() + "; Fallback also failed: " + ex.getMessage(), e);
            }
        }
    }
    
    /**
     * Convert DOM Node thành OMMLNode (đệ quy)
     */
    private static OMMLNode convertDOMNodeToOMMLNode(org.w3c.dom.Node domNode) {
        if (domNode == null) {
            return null;
        }
        
        switch (domNode.getNodeType()) {
            case Node.ELEMENT_NODE:
                Element element = (Element) domNode;
                String tagName = element.getTagName();
                
                // Remove namespace prefix for consistency
                String localName = tagName;
                if (tagName.contains(":")) {
                    localName = tagName.substring(tagName.indexOf(":") + 1);
                    // Keep full tag name with prefix for OMML
                    tagName = OMML_PREFIX + ":" + localName;
                }
                
                OMMLNode ommlNode = new OMMLNode(tagName);
                
                // Copy attributes
                if (element.hasAttributes()) {
                    org.w3c.dom.NamedNodeMap attributes = element.getAttributes();
                    for (int i = 0; i < attributes.getLength(); i++) {
                        org.w3c.dom.Node attr = attributes.item(i);
                        ommlNode.setAttribute(attr.getNodeName(), attr.getNodeValue());
                    }
                }
                
                // Process children (đệ quy)
                NodeList children = element.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    org.w3c.dom.Node child = children.item(i);
                    OMMLNode childOMMLNode = convertDOMNodeToOMMLNode(child);
                    if (childOMMLNode != null) {
                        ommlNode.addChild(childOMMLNode);
                    }
                }
                
                return ommlNode;
                
            case Node.TEXT_NODE:
                Text textNode = (Text) domNode;
                String text = textNode.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    OMMLNode textOMMLNode = new OMMLNode("m:t", text);
                    return textOMMLNode;
                }
                return null;
                
            default:
                // Ignore other node types (comments, processing instructions, etc.)
                return null;
        }
    }
    
    /**
     * Fallback: Parse using Apache XMLBeans XmlObject
     */
    private static OMMLNode parseWithXmlObject(String ommlXml) throws Exception {
        // Parse XML using XmlObject
        XmlObject xmlObject = XmlObject.Factory.parse(ommlXml);
        
        // Convert to OMMLNode using XmlCursor
        XmlCursor cursor = xmlObject.newCursor();
        try {
            cursor.toStartDoc();
            return convertXmlCursorToOMMLNode(cursor);
        } finally {
            cursor.close();
        }
    }
    
    /**
     * Convert XmlCursor position to OMMLNode (đệ quy)
     */
    private static OMMLNode convertXmlCursorToOMMLNode(XmlCursor cursor) {
        if (cursor == null) {
            return null;
        }
        
        // Get current element name
        if (cursor.getName() == null) {
            return null;
        }
        
        String elementName = cursor.getName().getLocalPart();
        String prefix = cursor.getName().getPrefix();
        String fullName = (prefix != null && !prefix.isEmpty()) ? prefix + ":" + elementName : elementName;
        
        OMMLNode node = new OMMLNode(fullName);
        
        // Get attributes
        cursor.push();
        try {
            if (cursor.toFirstAttribute()) {
                do {
                    if (cursor.getName() != null) {
                        String attrName = cursor.getName().getLocalPart();
                        String attrValue = cursor.getTextValue();
                        if (attrName != null && attrValue != null) {
                            node.setAttribute(attrName, attrValue);
                        }
                    }
                } while (cursor.toNextAttribute());
            }
        } finally {
            cursor.pop();
        }
        
        // Check if this is a leaf node (no children)
        cursor.push();
        boolean hasChildren = false;
        try {
            hasChildren = cursor.toFirstChild();
        } finally {
            cursor.pop();
        }
        
        // Get text content if this is a text node (leaf node)
        String textValue = cursor.getTextValue();
        if (textValue != null && !textValue.trim().isEmpty() && !hasChildren) {
            // Leaf node with text
            node.setTextContent(textValue);
        }
        
        // Process children (đệ quy)
        cursor.push();
        try {
            if (cursor.toFirstChild()) {
                do {
                    OMMLNode child = convertXmlCursorToOMMLNode(cursor);
                    if (child != null) {
                        node.addChild(child);
                    }
                } while (cursor.toNextSibling());
            }
        } finally {
            cursor.pop();
        }
        
        return node;
    }
    
    /**
     * Parse OMML từ XmlObject (từ Apache POI)
     */
    public static OMMLNode parseFromXmlObject(XmlObject xmlObject) throws Exception {
        if (xmlObject == null) {
            throw new IllegalArgumentException("XmlObject cannot be null");
        }
        
        try {
            // Get XML string from XmlObject
            String ommlXml = xmlObject.xmlText();
            return parse(ommlXml);
        } catch (Exception e) {
            // Fallback: Use XmlCursor
            XmlCursor cursor = xmlObject.newCursor();
            try {
                cursor.toStartDoc();
                return convertXmlCursorToOMMLNode(cursor);
            } finally {
                cursor.close();
            }
        }
    }
}


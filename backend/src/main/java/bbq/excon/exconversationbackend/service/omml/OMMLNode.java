package bbq.excon.exconversationbackend.service.omml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Đại diện cho một node trong OMML XML tree
 * Sử dụng tree structure để xử lý OMML một cách đệ quy
 * 
 * @author AI Assistant
 */
public class OMMLNode {
    private String tagName;              // Tên tag (ví dụ: "m:oMath", "m:f", "m:num")
    private String textContent;          // Text content nếu là text node
    private List<OMMLNode> children;     // Children nodes
    private Map<String, String> attributes; // Attributes của node
    private OMMLNode parent;             // Parent node (để traverse ngược lại)
    
    public OMMLNode(String tagName) {
        this.tagName = tagName;
        this.children = new ArrayList<>();
        this.attributes = new HashMap<>();
        this.textContent = null;
        this.parent = null;
    }
    
    public OMMLNode(String tagName, String textContent) {
        this(tagName);
        this.textContent = textContent;
    }
    
    // Getters and Setters
    public String getTagName() {
        return tagName;
    }
    
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
    
    public String getTextContent() {
        return textContent;
    }
    
    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }
    
    public List<OMMLNode> getChildren() {
        return children;
    }
    
    public void addChild(OMMLNode child) {
        if (child != null) {
            child.setParent(this);
            this.children.add(child);
        }
    }
    
    public void addChildren(List<OMMLNode> children) {
        if (children != null) {
            for (OMMLNode child : children) {
                addChild(child);
            }
        }
    }
    
    public Map<String, String> getAttributes() {
        return attributes;
    }
    
    public void setAttribute(String key, String value) {
        this.attributes.put(key, value);
    }
    
    public String getAttribute(String key) {
        return this.attributes.get(key);
    }
    
    public OMMLNode getParent() {
        return parent;
    }
    
    public void setParent(OMMLNode parent) {
        this.parent = parent;
    }
    
    /**
     * Tìm children nodes theo tag name
     */
    public List<OMMLNode> findChildrenByTag(String tagName) {
        List<OMMLNode> result = new ArrayList<>();
        for (OMMLNode child : children) {
            if (tagName.equals(child.getTagName())) {
                result.add(child);
            }
        }
        return result;
    }
    
    /**
     * Tìm child node đầu tiên theo tag name
     */
    public OMMLNode findFirstChildByTag(String tagName) {
        for (OMMLNode child : children) {
            if (tagName.equals(child.getTagName())) {
                return child;
            }
        }
        return null;
    }
    
    /**
     * Kiểm tra xem node có phải là text node không
     */
    public boolean isTextNode() {
        return textContent != null && !textContent.isEmpty();
    }
    
    /**
     * Lấy tất cả text content từ node và children (đệ quy)
     */
    public String getAllTextContent() {
        if (isTextNode()) {
            return textContent;
        }
        
        StringBuilder sb = new StringBuilder();
        for (OMMLNode child : children) {
            sb.append(child.getAllTextContent());
        }
        return sb.toString();
    }
    
    /**
     * Debug: In ra cấu trúc tree
     */
    public String toString(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
        sb.append("<").append(tagName);
        if (!attributes.isEmpty()) {
            for (Map.Entry<String, String> attr : attributes.entrySet()) {
                sb.append(" ").append(attr.getKey()).append("=\"").append(attr.getValue()).append("\"");
            }
        }
        sb.append(">");
        if (textContent != null) {
            sb.append(textContent);
        }
        sb.append("\n");
        
        for (OMMLNode child : children) {
            sb.append(child.toString(indent + 1));
        }
        
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
        sb.append("</").append(tagName).append(">\n");
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return toString(0);
    }
}


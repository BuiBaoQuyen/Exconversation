package bbq.excon.exconversationbackend.service.export;

/**
 * Represents a segment of content - either plain text or OMML equation
 */
public class ContentSegment {
    public enum Type {
        TEXT,
        OMML
    }
    
    private Type type;
    private String content;
    
    public ContentSegment(Type type, String content) {
        this.type = type;
        this.content = content;
    }
    
    public Type getType() {
        return type;
    }
    
    public String getContent() {
        return content;
    }
    
    public boolean isText() {
        return type == Type.TEXT;
    }
    
    public boolean isOMML() {
        return type == Type.OMML;
    }
}

















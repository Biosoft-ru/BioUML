package biouml.workbench.diagram;

import java.beans.PropertyChangeListener;

public interface DiagramTextRepresentation
{
    /**
     * @return content type string
     */
    String getContentType();
    
    /**
     * @return text content of the Diagram
     */
    String getContent();
    
    /**
     * @param text text content of the Diagram to set
     */
    void setContent(String text);
    
    void addTextChangeListener(PropertyChangeListener l);
}

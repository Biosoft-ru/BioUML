package biouml.plugins.physicell;

import java.awt.Dimension;
import java.awt.Point;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.InitialElementPropertiesSupport;
import biouml.model.Node;
import biouml.model.Role;
import biouml.standard.type.Stub;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.DPSUtils;

public class EventProperties extends InitialElementPropertiesSupport implements PhysicellRole
{
    private DiagramElement diagramElement;
    private double executionTime;
    private boolean useCustomExecutionCode;
    private DataElementPath executionCodePath;
    private String comment;
    private boolean isCompleted;
    private String name;
    private boolean showCode;
    private boolean formatCode;
    
    public EventProperties(DiagramElement de)
    {
        this.diagramElement = de;
        this.name = de.getName();
        this.isCompleted = true;
    }

    public EventProperties(String name)
    {
        this.name = name;
        this.isCompleted = false;
    }

    public boolean isCompleted()
    {
        return isCompleted;
    }
    
    @PropertyName("Execution time")
    public double getExecutionTime()
    {
        return executionTime;
    }
    public void setExecutionTime(double time)
    {
        this.executionTime = time;
    }
    
    @PropertyName("Custom Execution code")
    public boolean isUseCustomExecutionCode()
    {
        return useCustomExecutionCode;
    }
    public void setUseCustomExecutionCode(boolean customCode)
    {
        this.useCustomExecutionCode = customCode;
    }
    
    @PropertyName("Execution code")
    public DataElementPath getExecutionCodePath()
    {
        return executionCodePath;
    }
    public void setExecutionCodePath(DataElementPath path)
    {
        this.executionCodePath = path;
    }
    
    @PropertyName("Comment")
    public String getComment()
    {
        return comment;
    }
    public void setComment(String comment)
    {
        this.comment = comment;
    }
    
    @PropertyName ( "Name" )
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    
    @Override
    public DiagramElement getDiagramElement()
    {
        return diagramElement;
    }
    public void setDiagramElement(DiagramElement de)
    {
        this.diagramElement = de;
    }
    
    @Override
    public Role clone(DiagramElement de)
    {
        EventProperties result = new EventProperties(de);
        result.comment = comment;
        result.executionTime = executionTime;
        result.useCustomExecutionCode = useCustomExecutionCode;
        result.executionCodePath = executionCodePath;
        return result;
    }
    
    @Override
    public DiagramElementGroup doCreateElements(Compartment c, Point location, ViewEditorPane viewPane) throws Exception
    {
        Node result = new Node( c, new Stub( null, name, PhysicellConstants.TYPE_EVENT ) );
        result.setShapeSize( new Dimension( 75, 75 ) );
        result.setLocation( location );
        this.isCompleted = true;
        this.setDiagramElement( result );
        result.setRole( this );
        result.getAttributes().add( DPSUtils.createHiddenReadOnly( "event", EventProperties.class, this ) );
        return new DiagramElementGroup( result );
    }

    @PropertyName("Show code")
    public boolean isShowCode()
    {
        return showCode;
    }

    public void setShowCode(boolean showCode)
    {
        this.showCode = showCode;
    }

    @PropertyName("Format code")
    public boolean isFormatCode()
    {
        return formatCode;
    }

    public void setFormatCode(boolean formatCode)
    {
        this.formatCode = formatCode;
    }
}
package biouml.plugins.state.analyses;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.JSONBean;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Diagram;
import biouml.model.DiagramElement;

import com.developmentontheedge.beans.Option;

public class StateChange extends Option implements JSONBean
{
    private String elementId = "";
    @PropertyName ( "Element" )
    @PropertyDescription ( "Element id" )
    public String getElementId()
    {
        return elementId;
    }
    public void setElementId(String elementId)
    {
        String oldValue = this.elementId;
        this.elementId = elementId;
        firePropertyChange( "elementId", oldValue, elementId );
    }

    private String elementProperty;
    @PropertyName ( "Property" )
    @PropertyDescription ( "Element property" )
    public String getElementProperty()
    {
        return elementProperty;
    }

    public void setElementProperty(String elementProperty)
    {
        String oldValue = this.elementProperty;
        this.elementProperty = elementProperty;
        firePropertyChange( "elementProperty", oldValue, elementProperty );
        try
        {
            Diagram diagram = getDiagram();
            DiagramElement bean = diagram.getDiagramElement( getElementId() );
            Object value = BeanUtil.getBeanPropertyValue( bean, elementProperty );
            setPropertyValue( TextUtil.toString( value ) );
        }
        catch( Exception ignore )
        {

        }
    }

    private String propertyValue;
    @PropertyName ( "Value" )
    @PropertyDescription ( "Property value" )
    public String getPropertyValue()
    {
        return propertyValue;
    }
    public void setPropertyValue(String propertyValue)
    {
        String oldValue = this.propertyValue;
        this.propertyValue = propertyValue;
        firePropertyChange( "propertyValue", oldValue, propertyValue );
    }

    Diagram getDiagram()
    {
        DiagramAndChanges parameters = (DiagramAndChanges)getParent();
        if( parameters == null )
            return null;
        DataElementPath diagramPath = parameters.getDiagramPath();
        if( diagramPath == null )
            return null;
        return diagramPath.optDataElement( Diagram.class );
    }
}

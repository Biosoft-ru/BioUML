package biouml.plugins.state.analyses;

import java.util.Map;

import ru.biosoft.util.BeanAsMapUtil;
import biouml.model.Diagram;
import biouml.model.DiagramElement;

import com.developmentontheedge.beans.editors.StringTagEditor;
import com.developmentontheedge.beans.model.Property;

public class DiagramElementPropertySelector extends StringTagEditor
{
    @Override
    public String[] getTags()
    {
        StateChange bean = (StateChange)getBean();
        Diagram diagram = bean.getDiagram();
        if( diagram == null )
            return null;

        if( bean.getElementId() == null )
            return null;

        DiagramElement source = diagram.getDiagramElement( bean.getElementId() );
        if(source == null)
            return null;
        Map<String, Object> propertyMap = BeanAsMapUtil.convertBeanToMap( source,
                p -> p.getCompleteName().equals( "role/vars" ) || ( !p.isReadOnly() && p.isVisible( Property.SHOW_EXPERT ) ) );
        Map<String, Object> flatPropertyMap = BeanAsMapUtil.flattenMap( propertyMap );
        return flatPropertyMap.keySet().toArray( new String[flatPropertyMap.size()] );
    }
}

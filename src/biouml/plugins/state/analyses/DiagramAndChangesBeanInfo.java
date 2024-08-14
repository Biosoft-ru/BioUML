package biouml.plugins.state.analyses;

import com.developmentontheedge.beans.BeanInfoEx;

import ru.biosoft.access.repository.DataElementPathEditor;
import biouml.model.Diagram;

public class DiagramAndChangesBeanInfo extends BeanInfoEx
{
    protected DiagramAndChangesBeanInfo(Class<?> beanClass)
    {
        super( beanClass, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add( DataElementPathEditor.registerInput( "diagramPath", beanClass, Diagram.class ) );
        add( "changes" );
    }
}

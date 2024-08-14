package biouml.model;

import java.beans.IntrospectionException;

import ru.biosoft.graphics.editor.FontEditor;
import ru.biosoft.graphics.PenEditor;
import ru.biosoft.util.bean.BeanInfoEx2;

public class DiagramElementStyleBeanInfo extends BeanInfoEx2<DiagramElementStyle>
{
    public DiagramElementStyleBeanInfo()
    {
        super(DiagramElementStyle.class);
    }

    @Override
    public void initProperties() throws IntrospectionException
    {
        addWithoutChildren( "pen", PenEditor.class );
        add( "brush" );
        add( "font", FontEditor.class );
    }
}

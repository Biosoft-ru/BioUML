package biouml.workbench.diagram.viewpart;

import ru.biosoft.util.bean.BeanInfoEx2;

public class ConnectionSimpleBeanInfo extends BeanInfoEx2<ConnectionSimple>
{
    public ConnectionSimpleBeanInfo()
    {
        super( ConnectionSimple.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        addReadOnly( "name" );
        addReadOnly( "type" );
        addReadOnly( "moduleFrom" );
        addReadOnly( "variableFrom" );
        addReadOnly( "moduleTo" );
        addReadOnly( "variableTo" );
    }
}
package biouml.workbench.diagram.viewpart;

import ru.biosoft.util.bean.BeanInfoEx2;

public class PortSimpleBeanInfo extends BeanInfoEx2<PortSimple>
{
    public PortSimpleBeanInfo()
    {
        super( PortSimple.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        addReadOnly( "name" );
        add( "title" );
        addReadOnly( "type" );
        addReadOnly( "accessType" );
        addReadOnly( "variable" );
        addReadOnly( "module" );
    }
}
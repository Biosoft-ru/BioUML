package biouml.plugins.physicell;

import ru.biosoft.util.bean.BeanInfoEx2;

public class DomainOptionsBeanInfo extends BeanInfoEx2<DomainOptions>
{
    public DomainOptionsBeanInfo()
    {
        super( DomainOptions.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        add( "xFrom" );
        add( "yFrom" );
        addHidden( "zFrom", "isUse2D" );
        add( "xTo" );
        add( "yTo" );
        addHidden( "zTo", "isUse2D" );
        add( "xStep" );
        add( "yStep" );
        addHidden( "zStep", "isUse2D" );
        add( "use2D" );
    }
}
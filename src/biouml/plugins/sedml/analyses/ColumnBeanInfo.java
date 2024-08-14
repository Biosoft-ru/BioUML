package biouml.plugins.sedml.analyses;

import ru.biosoft.util.bean.BeanInfoEx2;

public class ColumnBeanInfo extends BeanInfoEx2<Column>
{
    public ColumnBeanInfo()
    {
        super( Column.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add( "name" );
        add( "expression" );
    }
}

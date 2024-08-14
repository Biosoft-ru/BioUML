package biouml.plugins.riboseq.ingolia;

import ru.biosoft.table.TableDataCollection;

public class CoreParametersWithASiteTableBeanInfo extends CoreParametersBeanInfo
{

    protected CoreParametersWithASiteTableBeanInfo(Class<? extends CoreParametersWithASiteTable> beanClass)
    {
        super( beanClass );
    }
    
    public CoreParametersWithASiteTableBeanInfo()
    {
        this(CoreParametersWithASiteTable.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        property("aSiteOffsetTable").inputElement( TableDataCollection.class ).canBeNull().add();;
    }
    
}

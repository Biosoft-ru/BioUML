package biouml.plugins.riboseq.ingolia.asite;

import biouml.plugins.riboseq.ingolia.CoreParametersBeanInfo;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;

public class BuildASiteOffsetTableParametersBeanInfo extends CoreParametersBeanInfo
{
    public BuildASiteOffsetTableParametersBeanInfo()
    {
        super( BuildASiteOffsetTableParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        add( DataElementPathEditor.registerOutput( "aSiteOffsetTable", beanClass, TableDataCollection.class ) );
    }
}

package biouml.plugins.riboseq.ingolia;

import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.table.TableDataCollection;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class PredictStartSitesParametersBeanInfo extends BasicParametersBeanInfo
{
    public PredictStartSitesParametersBeanInfo()
    {
        super( PredictStartSitesParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(DataElementPathEditor.registerInput( "modelFile", beanClass, FileDataElement.class ));
        super.initProperties();
        
        PropertyDescriptorEx pde = DataElementPathEditor.registerOutput( "summaryTable", beanClass, TableDataCollection.class );
        add(pde);
        property( "outputTrack" ).outputElement( SqlTrack.class ).add();
    }
}

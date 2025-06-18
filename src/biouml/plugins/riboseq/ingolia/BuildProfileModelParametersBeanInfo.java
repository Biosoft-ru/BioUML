package biouml.plugins.riboseq.ingolia;

import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.table.TableDataCollection;

public class BuildProfileModelParametersBeanInfo extends BasicParametersBeanInfo
{
    public BuildProfileModelParametersBeanInfo()
    {
        super( BuildProfileModelParameters.class );
    }
    
    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        add( "learningFraction" );
        add( "randomSeed" );
        add( DataElementPathEditor.registerOutput( "modelFile", beanClass, FileDataElement.class ) );
        add( DataElementPathEditor.registerOutput( "confusionMatrix", beanClass, TableDataCollection.class ) );
    }
}

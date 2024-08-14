package biouml.plugins.seek;

import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SeekSyncParametersBeanInfo extends BeanInfoEx2<SeekSyncParameters>
{

    public SeekSyncParametersBeanInfo()
    {
        super( SeekSyncParameters.class );
    }

    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();

        property( "seekUrl" ).add();
        property( "login" ).add();
        property( "password" ).add();
        property( "outputPath" ).outputElement( FolderCollection.class ).add();
        property( "availableDataFiles" ).canBeNull().editor( DataFilesLister.class ).simple().hideChildren().add();
    }
}
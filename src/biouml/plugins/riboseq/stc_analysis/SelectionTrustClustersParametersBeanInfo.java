package biouml.plugins.riboseq.stc_analysis;

import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SelectionTrustClustersParametersBeanInfo extends BeanInfoEx2<SelectionTrustClustersParameters>
{
    public SelectionTrustClustersParametersBeanInfo()
    {
        super( SelectionTrustClustersParameters.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        property( "inputPath" ).inputElement( SqlTrack.class ).add();

        property( "pathToHousekeepingGenes" ).inputElement( TableDataCollection.class ).add();

        property( "outputPathYesTrack" ).outputElement( SqlTrack.class ).auto( "$inputPath$ Yes selection" ).add();

        property( "outputPathNoTrack" ).outputElement( SqlTrack.class ).auto( "$inputPath$ No selection" ).add();

        property( "outputPathUndefinedTrack" ).outputElement( SqlTrack.class ).auto( "$inputPath$ Undefined selection" ).add();
    }
}

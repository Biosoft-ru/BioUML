package biouml.plugins.riboseq.coverageChecker;

import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class CoverageCheckerParametersBeanInfo extends BeanInfoEx2<CoverageCheckerParameters>
{
    public CoverageCheckerParametersBeanInfo()
    {
        super( CoverageCheckerParameters.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        property( "inputBamTrackPath" ).inputElement( BAMTrack.class ).add();
        property( "inputArticleCSV" ).inputElement( TableDataCollection.class ).add();
        property( "inputGeneInfo" ).inputElement( TableDataCollection.class ).add();

        property( "outputStatisticTablePath" ).outputElement( TableDataCollection.class ).auto( "$inputBamTrackPath$ coverageStat" ).add();
    }
}

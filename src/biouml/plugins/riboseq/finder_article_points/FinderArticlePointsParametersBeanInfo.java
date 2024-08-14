package biouml.plugins.riboseq.finder_article_points;

import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class FinderArticlePointsParametersBeanInfo extends BeanInfoEx2<FinderArticlePointsParameters>
{
    public FinderArticlePointsParametersBeanInfo()
    {
        super( FinderArticlePointsParameters.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        property( "inputArticlePointTable" ).inputElement( TableDataCollection.class ).add();
        property( "inputAllClusterTrack" ).inputElement( SqlTrack.class ).add();
        property( "inputFilteredTrack" ).inputElement( SqlTrack.class ).add();
        property( "inputSvmYesTrack" ).inputElement( SqlTrack.class ).add();

        property( "outputStatisticTable" ).outputElement( TableDataCollection.class ).auto( "$inputArticlePointTable$ finderStatistic" )
                .add();

    }
}

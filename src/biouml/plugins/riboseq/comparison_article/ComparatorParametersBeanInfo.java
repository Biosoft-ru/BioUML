package biouml.plugins.riboseq.comparison_article;

import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ComparatorParametersBeanInfo extends BeanInfoEx2<ComparatorParameters>
{
    public ComparatorParametersBeanInfo()
    {
        super( ComparatorParameters.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        property( "inputArticleCSV" ).inputElement( TableDataCollection.class ).add();
        property( "inputGeneInfo" ).inputElement( TableDataCollection.class ).add();
        property( "inputYesTrack" ).inputElement( SqlTrack.class ).add();

        property( "outputStatistic" ).outputElement( TableDataCollection.class ).auto( "$inputYesTrack$ comparison" ).add();
        property( "outputUniqueArticlePointTable" ).outputElement( TableDataCollection.class ).auto( "$inputYesTrack$ uniqueArticlePoint" )
                .add();
    }
}

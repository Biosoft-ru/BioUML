package biouml.plugins.riboseq.article_track;

import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ArticleTrackCreatorParametersBeanInfo extends BeanInfoEx2<ArticleTrackCreatorParameters>
{
    public ArticleTrackCreatorParametersBeanInfo()
    {
        super( ArticleTrackCreatorParameters.class );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();

        property( "inputArticleCSV" ).inputElement( TableDataCollection.class ).add();
        property( "inputGeneInfo" ).inputElement( TableDataCollection.class ).add();

        property( "inputReferenceTrack" ).inputElement( SqlTrack.class ).add();

        property( "outputTrack" ).outputElement( TableDataCollection.class ).auto( "$inputArticleCSV$ track" ).add();
    }
}

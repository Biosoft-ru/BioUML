package biouml.standard.type.access;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.HtmlUtil;

/**
 * @author lan
 */
public class TitleSqlNoHtmlIndex extends TitleSqlIndex
{
    /**
     * @param dc
     * @param indexName
     * @throws Exception
     */
    public TitleSqlNoHtmlIndex(DataCollection dc, String indexName) throws Exception
    {
        super(dc, indexName);
    }

    @Override
    protected String sanitizeTitle(String title)
    {
        return HtmlUtil.stripHtml(title);
    }
}

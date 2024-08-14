package biouml.plugins.reactome.access;

import ru.biosoft.access.core.DataCollection;
import biouml.standard.type.access.TitleSqlIndex;

public class ReactomePathwayTitleIndex extends TitleSqlIndex
{
    public ReactomePathwayTitleIndex(DataCollection dc, String indexName) throws Exception
    {
        super(dc, indexName);
    }

    @Override
    protected String makeQuery(DataCollection dc) throws Exception
    {
        return "select p2r.db_id,do._displayName from PathwayDiagram_2_representedPathway p2r join DatabaseObject do on p2r.representedPathway=do.DB_ID and representedPathway_rank=0";
    }
}

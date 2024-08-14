package biouml.plugins.ensembl.access;

import java.sql.Connection;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.sql.SqlUtil;
import biouml.standard.type.access.TitleSqlIndex;

@SuppressWarnings ( "serial" )
public class EnsemblIndex extends TitleSqlIndex
{
    public EnsemblIndex(DataCollection<?> dc, String indexName) throws Exception
    {
        super(dc, indexName);
    }

    @Override
    protected void doInit()
    {
        String ensClass   = dc.getClass().getSimpleName();
        String idQuery    = "SELECT t1.stable_id FROM gene_stable_id AS t1 INNER JOIN gene AS t2 ON t1.gene_id = t2.gene_id WHERE t2.display_xref_id = NULL";
        String labelQuery = "SELECT t1.display_label, t3.stable_id FROM xref AS t1 INNER JOIN gene AS t2 ON t1.xref_id = t2.display_xref_id INNER JOIN gene_stable_id AS t3 ON t2.gene_id = t3.gene_id";
        if (ensClass.endsWith("EnsemblGeneDataCollection2"))
        {
            idQuery    = "SELECT t1.stable_id FROM gene AS t1 INNER JOIN gene AS t2 ON t1.gene_id = t2.gene_id WHERE t2.display_xref_id = NULL";
            labelQuery = "SELECT t1.display_label, t3.stable_id FROM xref AS t1 INNER JOIN gene AS t2 ON t1.xref_id = t2.display_xref_id INNER JOIN gene AS t3 ON t2.gene_id = t3.gene_id";
        }
        EnsemblGeneDataCollection ensDC = (EnsemblGeneDataCollection)dc;
        Connection connection = ensDC.getConnection();
        for(String str: SqlUtil.queryStrings(connection, idQuery))
            putInternal(str, str);

        SqlUtil.iterate( connection, labelQuery, rs -> putInternal( rs.getString( 2 ), rs.getString( 1 ) ) );
    }
}

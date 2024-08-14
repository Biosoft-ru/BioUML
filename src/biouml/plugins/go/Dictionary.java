package biouml.plugins.go;

import java.util.Map.Entry;
import java.util.Properties;

import biouml.standard.type.DatabaseInfo;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.ReadOnlyVectorCollection;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;

public class Dictionary extends ReadOnlyVectorCollection<DatabaseInfo>
{
    public Dictionary(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);
    }

    @Override
    protected void doInit()
    {
        for(Entry<String, String> entry : SqlUtil.queryMap( SqlConnectionPool.getConnection(this), "SELECT name, url_syntax FROM db").entrySet())
        {
            String urlSyntax = entry.getValue();
            if( urlSyntax != null && urlSyntax.indexOf('[') == urlSyntax.lastIndexOf('[') )
            {
                String url = urlSyntax.replace("[example_id]", "$id$");
                DatabaseInfo dbi = new DatabaseInfo(this, entry.getKey());
                dbi.setQueryByAc(url);
                dbi.setQueryById(url);
                doPut(dbi, true);
            }
        }
    }
}

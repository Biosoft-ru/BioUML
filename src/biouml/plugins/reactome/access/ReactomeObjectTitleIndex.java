package biouml.plugins.reactome.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformer;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.sql.SqlConnectionHolder;
import biouml.standard.type.access.TitleSqlIndex;

@SuppressWarnings ( "serial" )
public class ReactomeObjectTitleIndex extends TitleSqlIndex
{
    public ReactomeObjectTitleIndex(DataCollection<?> dc, String indexName) throws Exception
    {
        super(dc, indexName);
    }

    @Override
    protected void doInit() throws Exception
    {
        SecurityManager.runPrivileged(() -> {
            DataCollection<?> primary = DataCollectionUtils.fetchPrimaryCollectionPrivileged(dc);
            if( primary == null || ! ( primary instanceof SqlDataCollection ) )
                return null;
            
            SqlTransformer<?> tr = ((SqlDataCollection<?>)primary).getTransformer();
            if(!(tr instanceof ReactomeObjectSqlTransformer))
                return null;
            String reactomeClassName = ((ReactomeObjectSqlTransformer<?>)tr).getReactomeObjectClass();
            if(reactomeClassName == null)
                return null;
            
            SqlConnectionHolder sqlDC = (SqlConnectionHolder)primary;
            Connection connection = sqlDC.getConnection();

            String query = "SELECT DISTINCT identifier, _displayName FROM DatabaseObject dbt INNER JOIN StableIdentifier si"
            + " ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='" + reactomeClassName + "'";
            try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery( query ))
            {
                while( rs.next() )
                    putInternal( rs.getString( 1 ), rs.getString( 2 ) );
            }
            return null;
        });
    }
}

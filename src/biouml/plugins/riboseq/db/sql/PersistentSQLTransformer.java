package biouml.plugins.riboseq.db.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.SqlTransformerSupport;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlUtil;


public abstract class PersistentSQLTransformer<T extends DataElement> extends SqlTransformerSupport<T>
{
    public PersistentSQLTransformer()
    {
        String className = getTemplateClass().getSimpleName();
        String sqlName = className.replaceAll( "([a-z0-9])([A-Z])", "$1_$2" ).toLowerCase();
        table = SqlUtil.quoteIdentifier( sqlName );
        idField = sqlName + "_id";
    }
    
    @Override
    public T create(ResultSet resultSet, Connection connection) throws Exception
    {
        String id = resultSet.getString( idField );
        T result = getTemplateClass().getConstructor( ru.biosoft.access.core.DataCollection.class, String.class ).newInstance( owner, id );
        return result;
    }
    
    protected abstract Query getInsertQuery(T de);
    
    @Override
    public void addInsertCommands(Statement statement, T de) throws Exception
    {
        statement.addBatch( getInsertQuery(de).get() );
    }
}

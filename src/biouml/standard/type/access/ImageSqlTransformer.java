
package biouml.standard.type.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.SqlTransformerSupport;

public class ImageSqlTransformer extends SqlTransformerSupport<DataElementSupport>
{
    @Override
    public boolean init(SqlDataCollection<DataElementSupport> owner)
    {
        table = "images";
        this.owner = owner;
        return true;
    }

    @Override
    public Class<DataElementSupport> getTemplateClass()
    {
        return DataElementSupport.class;
    }

    @Override
    public String getSelectQuery()
    {
        return "SELECT id FROM " + table;
    }

    @Override
    public DataElementSupport create(ResultSet resultSet, Connection connection) throws Exception
    {
        return new DataElementSupport(resultSet.getString(1), owner);
    }

    @Override
    public void addInsertCommands(Statement statement, DataElementSupport de) throws SQLException, Exception
    {
    }
    @Override
    public void addUpdateCommands(Statement statement, DataElementSupport de) throws Exception
    {
    }
}

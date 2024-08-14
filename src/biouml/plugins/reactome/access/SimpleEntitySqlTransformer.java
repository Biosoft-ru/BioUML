package biouml.plugins.reactome.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.biosoft.access.SqlDataCollection;
import biouml.standard.type.Substance;

public class SimpleEntitySqlTransformer extends PhysicalEntitySqlTransformer<Substance>
{
    @Override
    public boolean init(SqlDataCollection<Substance> owner)
    {
        //table = "SimpleEntity";
        //idField = "DB_ID";
        table = "StableIdentifier";
        idField = "identifier";
        this.owner = owner;
        return true;
    }

    @Override
    public Substance create(ResultSet resultSet, Connection connection) throws Exception
    {
        return super.create(resultSet, connection);
    }
    
    @Override
    public String getNameListQuery()
    {
        return "SELECT " + idField + " FROM " + table + " si INNER JOIN " + databaseObjectTable
        + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='SimpleEntity' ORDER BY " + idField;

    }
    
    @Override
    public String getCountQuery()
    {
        return "SELECT count(" + idField + ") FROM " + table + " si INNER JOIN " + databaseObjectTable
        + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='SimpleEntity'";
    }

    @Override
    protected Substance createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        return new Substance(owner, resultSet.getString(1));
    }

    @Override
    public Class<Substance> getTemplateClass()
    {
        return Substance.class;
    }
    
    @Override
    protected String getReactomeObjectClass()
    {
        return "SimpleEntity";
    }
}

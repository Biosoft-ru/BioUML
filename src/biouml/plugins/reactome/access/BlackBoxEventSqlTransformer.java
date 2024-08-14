
package biouml.plugins.reactome.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.biosoft.access.SqlDataCollection;
import biouml.standard.type.Reaction;

/**
 * @author anna
 *
 */
public class BlackBoxEventSqlTransformer extends ReactionLikeEventSqlTransformer
{
    @Override
    public boolean init(SqlDataCollection<Reaction> owner)
    {
        //table = "BlackBoxEvent";
        //idField = "DB_ID";
        table = "StableIdentifier";
        idField = "identifier";
        this.owner = owner;
        return true;
    }

    @Override
    protected Reaction createElement(ResultSet resultSet, Connection connection) throws SQLException
    {
        return new Reaction(owner, resultSet.getString(1));
    }

    @Override
    public String getNameListQuery()
    {
        return "SELECT " + idField + " FROM " + table + " si INNER JOIN " + databaseObjectTable
                + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='BlackBoxEvent' ORDER BY " + idField;
    }

    @Override
    public String getCountQuery()
    {
        return "SELECT count(" + idField + ") FROM " + table + " si INNER JOIN " + databaseObjectTable
                + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='BlackBoxEvent'";
    }

    @Override
    protected String getReactomeObjectClass()
    {
        return "BlackBoxEvent";
    }
}

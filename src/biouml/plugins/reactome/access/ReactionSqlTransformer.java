package biouml.plugins.reactome.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import biouml.standard.type.Reaction;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlUtil;

public class ReactionSqlTransformer extends ReactionLikeEventSqlTransformer
{
    private static final Query REVERSE_REACTION_QUERY = new Query("SELECT DISTINCT reverseReaction FROM Reaction WHERE DB_ID=$id$");
    
    @Override
    public boolean init(SqlDataCollection<Reaction> owner)
    {
        //table = "Reaction";
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
    public Reaction create(ResultSet resultSet, Connection connection) throws Exception
    {
        Reaction reaction = super.create(resultSet, connection);
        if(SqlUtil.hasResult( connection, REVERSE_REACTION_QUERY.str( getReactomeId(reaction) )))
            reaction.setReversible(true);
        return reaction;
    }
    
    @Override
    public String getNameListQuery()
    {
        return "SELECT " + idField + " FROM " + table + " si INNER JOIN " + databaseObjectTable
        + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='Reaction' ORDER BY " + idField;
    }
    
    @Override
    public String getCountQuery()
    {
        return "SELECT count(" + idField + ") FROM " + table + " si INNER JOIN " + databaseObjectTable
        + " dbt ON(dbt.stableIdentifier=si.DB_ID) WHERE dbt._class='Reaction'";
    }

    @Override
    protected String getReactomeObjectClass()
    {
        return "Reaction";
    }
}

package biouml.plugins.riboseq.db.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import biouml.plugins.riboseq.db.model.Condition;
import ru.biosoft.access.sql.Query;

public class ConditionSQLTransformer extends PersistentSQLTransformer<Condition>
{
    @Override
    public Class<Condition> getTemplateClass()
    {
        return Condition.class;
    }

    @Override
    public Condition create(ResultSet resultSet, Connection connection) throws Exception
    {
        Condition result = super.create( resultSet, connection );
        result.setDescription( resultSet.getString( "description" ) );
        return result;
    }

    @Override
    protected Query getInsertQuery(Condition de)
    {
        return new Query( "INSERT INTO $table$ (condition_id,description) VALUES($condition_id$,$description$)" )
            .name( "table", "condition" )
            .str( "condition_id", de.getName() )
            .str( "description", de.getDescription() );
    }
    
    @Override
    public void addUpdateCommands(Statement statement, Condition de) throws Exception
    {
        Query query = new Query( "UPDATE $table$ SET description=$description$ WHERE condition_id=$condition_id$" )
                .name( "table", "condition" )
                .str( "description", de.getDescription() )
                .str( "condition_id", de.getName() );
        statement.addBatch( query.get() );
    }
}

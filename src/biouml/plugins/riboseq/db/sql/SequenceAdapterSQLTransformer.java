package biouml.plugins.riboseq.db.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import biouml.plugins.riboseq.db.model.SequenceAdapter;
import ru.biosoft.access.sql.Query;

public class SequenceAdapterSQLTransformer extends PersistentSQLTransformer<SequenceAdapter>
{
    @Override
    public Class<SequenceAdapter> getTemplateClass()
    {
        return SequenceAdapter.class;
    }

    @Override
    public SequenceAdapter create(ResultSet resultSet, Connection connection) throws Exception
    {
        SequenceAdapter result = super.create( resultSet, connection );
        result.setTitle( resultSet.getString( "title" ) );
        result.setSequence( resultSet.getString( "sequence" ) );
        return result;
    }

    @Override
    protected Query getInsertQuery(SequenceAdapter de)
    {
        return new Query( "INSERT INTO $table$ (sequence_adapter_id,title,sequence) VALUES($sequence_adapter_id$,$title$,$sequence$)" )
            .name( "table", "sequence_adapter" )
            .str( "sequence_adapter_id", de.getName() )
            .str( "title", de.getTitle() )
            .str( "sequence", de.getSequence() );
    }
    
    @Override
    public void addUpdateCommands(Statement statement, SequenceAdapter de) throws Exception
    {
        Query query = new Query( "UPDATE $table$ SET title=$title$, sequence=$sequence$ WHERE sequence_adapter_id=$sequence_adapter_id$" )
                .name( "table", "sequence_adapter" )
                .str( "title", de.getTitle() )
                .str( "sequence", de.getSequence() )
                .str( "sequence_adapter_id", de.getName() );
        statement.addBatch( query.get() );
    }

}

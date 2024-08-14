package biouml.plugins.riboseq.db.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import biouml.plugins.riboseq.db.model.SequencingPlatform;
import ru.biosoft.access.sql.Query;

public class SequencingPlatformSQLTransformer extends PersistentSQLTransformer<SequencingPlatform>
{
    @Override
    public Class<SequencingPlatform> getTemplateClass()
    {
        return SequencingPlatform.class;
    }

    @Override
    public SequencingPlatform create(ResultSet resultSet, Connection connection) throws Exception
    {
        SequencingPlatform result = super.create( resultSet, connection );
        result.setTitle( resultSet.getString( "title" ) );
        return result;
    }

    @Override
    protected Query getInsertQuery(SequencingPlatform de)
    {
        return new Query( "INSERT INTO $table$ (sequencing_platform_id,title) VALUES($sequencing_platform_id$,$title$)" )
            .name( "table", "sequencing_platform" )
            .str( "sequencing_platform_id", de.getName() )
            .str( "title", de.getTitle() );
    }

    @Override
    public void addUpdateCommands(Statement statement, SequencingPlatform de) throws Exception
    {
        Query query = new Query( "UPDATE $table$ SET title=$title$ WHERE sequencing_platform_id=$sequencing_platform_id$" )
                .name( "table", "sequencing_platform" )
                .str( "title", de.getTitle() )
                .str( "sequencing_platform_id", de.getName() );
        statement.addBatch( query.get() );
    }
}

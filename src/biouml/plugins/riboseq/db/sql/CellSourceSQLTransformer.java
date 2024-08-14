package biouml.plugins.riboseq.db.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import ru.biosoft.access.sql.Query;
import biouml.plugins.riboseq.db.model.CellSource;

public class CellSourceSQLTransformer extends PersistentSQLTransformer<CellSource>
{
    @Override
    public Class<CellSource> getTemplateClass()
    {
        return CellSource.class;
    }

    @Override
    public CellSource create(ResultSet resultSet, Connection connection) throws Exception
    {
        CellSource result = super.create( resultSet, connection );
        result.setTitle( resultSet.getString( "title" ) );
        return result;
    }

    @Override
    protected Query getInsertQuery(CellSource de)
    {
        return new Query( "INSERT INTO $table$ (cell_source_id,title) VALUES($cell_source_id$,$title$)" )
            .name( "table", "cell_source" )
            .str( "cell_source_id", de.getName() )
            .str( "title", de.getTitle() );
    }
    
    @Override
    public void addUpdateCommands(Statement statement, CellSource de) throws Exception
    {
        Query query = new Query( "UPDATE $table$ SET title=$title$ WHERE cell_source_id=$cell_source_id$" )
                .name( "table", "cell_source" )
                .str( "cell_source_id", de.getName() )
                .str( "title", de.getTitle() );
        statement.addBatch( query.get() );
    }

}

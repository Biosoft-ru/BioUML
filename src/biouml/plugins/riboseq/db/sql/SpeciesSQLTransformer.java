package biouml.plugins.riboseq.db.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import ru.biosoft.access.sql.Query;
import biouml.plugins.riboseq.db.model.Species;

public class SpeciesSQLTransformer extends PersistentSQLTransformer<Species>
{
    @Override
    public Class<Species> getTemplateClass()
    {
        return Species.class;
    }

    @Override
    public Species create(ResultSet resultSet, Connection connection) throws Exception
    {
        Species result = super.create( resultSet, connection );
        result.setLatinName( resultSet.getString( "latin_name" ) );
        result.setCommonName( resultSet.getString( "common_name" ) );
        return result;
    }


    @Override
    protected Query getInsertQuery(Species de)
    {
        return new Query( "INSERT INTO $table$ (species_id,latin_name,common_name) VALUES($species_id$,$latin_name$,$common_name$)" )
            .name( "table", "species" )
            .str( "species_id", de.getName() )
            .str( "latin_name", de.getLatinName() )
            .str( "common_name", de.getCommonName() );
    }

    @Override
    public void addUpdateCommands(Statement statement, Species de) throws Exception
    {
        Query query = new Query( "UPDATE $table$ SET latin_name=$latin_name$, common_name=$common_name$ WHERE species_id=$species_id$" )
                .name( "table", "species" )
                .str( "latin_name", de.getLatinName() )
                .str( "common_name", de.getCommonName() )
                .str( "species_id", de.getName() );
        statement.addBatch( query.get() );
    }
}

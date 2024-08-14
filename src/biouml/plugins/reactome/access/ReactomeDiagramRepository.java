package biouml.plugins.reactome.access;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.sql.SqlConnectionPool;

public class ReactomeDiagramRepository extends LocalRepository
{
    public ReactomeDiagramRepository(DataCollection<?> parent, Properties properties) throws Exception
    {
        super( parent, properties );
        Connection connection;
        if( parent == null || ( connection = SqlConnectionPool.getConnection( parent ) ) == null )
            return;
        String query = "SELECT DISTINCT d.db_id,sl.name FROM BioUML_species_list sl"
                + " INNER JOIN BioUML_diagrams d ON (d.species=sl.db_id)";
        try( Statement st = connection.createStatement(); ResultSet rs = st.executeQuery( query ) )
        {
            while( rs.next() )
            {
                diagramId2Species.put( rs.getLong( 1 ) + "", rs.getString( 2 ) );
            }
        }
        catch( Throwable t )
        {
            //ignore
        }
    }

    private final Map<String, String> diagramId2Species = new HashMap<>();

    public String[] getInnerDiagramPathParts(String name)
    {
        String species = diagramId2Species.get( name );
        return species == null ? new String[] {name} : new String[] {species, name};
    }

    @Override
    protected DataCollection<?> doGet(String name)
    {
        DataCollection<?> result = super.doGet( name );
        if( result == null )
            result = getDiagramFromSubCollection( name );
        return result;
    }
    private DataCollection<?> getDiagramFromSubCollection(String name)
    {
        String species = diagramId2Species.get( name );
        try
        {
            if( species != null )
                return (DataCollection<?>)get( species ).get( name ); //try to get diagram from inner collection
        }
        catch( Exception e )
        {
        }
        return null;
    }
}

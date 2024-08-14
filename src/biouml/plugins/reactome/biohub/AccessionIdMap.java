package biouml.plugins.reactome.biohub;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import ru.biosoft.access.exception.BiosoftSQLException;

class AccessionIdMap
{
    private static Logger log = Logger.getLogger(AccessionIdMap.class.getName());

    private static final String QUERY = "SELECT dbt.DB_ID,identifier FROM DatabaseObject dbt INNER JOIN StableIdentifier si ON(dbt.stableIdentifier=si.DB_ID)";
    /**
     * Maps for accession numbers
     */
    TObjectIntMap<String> accToId = new TObjectIntHashMap<>( 16, 0.75f, 0 );
    TIntObjectMap<String> idToAcc = new TIntObjectHashMap<>( 16, 0.75f );

    public AccessionIdMap(Connection conn) throws BiosoftSQLException
    {
        initAccessionMap(conn);
    }
    
    private void initAccessionMap(Connection conn) throws BiosoftSQLException
    {
        try(Statement st = conn.createStatement();
                ResultSet resultSet = st.executeQuery(QUERY))
        {
            while(resultSet.next())
            {
                try
                {
                    int id = resultSet.getInt(1);
                    ReactomeElement acc = new ReactomeElement( resultSet.getString( 2 ) );
                    String accession = acc.getAccession();
                    accToId.put( accession, id );
                    idToAcc.put( id, accession );
                }
                catch( IllegalArgumentException e )
                {
                    log.log( Level.SEVERE,
                            "Problems initializing Reactome hub: " + resultSet.getString( 1 ) + ":" + resultSet.getString( 2 ), e );
                }
            }
        }
        catch(SQLException ex)
        {
            throw new BiosoftSQLException( conn, QUERY, ex );
        }
    }
    
    public int getMoleculeId(String acc)
    {
        return accToId.get( acc );
    }
    
    public String getMoleculeAcc(int id)
    {
        return idToAcc.get( id );
    }
}
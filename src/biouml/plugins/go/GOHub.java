package biouml.plugins.go;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.standard.type.DatabaseReference;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.exception.BioHubFetchException;
import ru.biosoft.access.sql.SqlConnectionPool;

public class GOHub extends BioHubSupport
{
    // TODO: hardcoded repository path
    public static final DataElementPath GO_PATH = DataElementPath.create("databases/GO");

    protected Logger log = Logger.getLogger(GOHub.class.getName());

    private static String GENESYMBOL_QUERY = "select g.symbol,g.id from gene_product g,species s where s.id=g.species_id and s.common_name='human' and symbol=?";
    private static String UNIPROT_QUERY = "select x.xref_key symbol,g.id from gene_product g,species s,dbxref x where s.id=g.species_id and s.common_name='human' and x.id=g.dbxref_id and x.xref_key=?";
    private static String TERM_QUERY = "select distinct t.acc from term t,association a where a.term_id=t.id and gene_product_id=?";

    public GOHub(Properties properties)
    {
        super(properties);
    }

    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        return getPriority( dbOptions, GO_PATH, () -> 10 );
    }

    @Override
    public Element[] getReference(Element startElement, TargetOptions constraints, String[] relationTypes, int maxLength, int direction)
    {
        if(constraints.getUsedCollectionPaths().isEmpty())
            return null;
        Connection connection = getConnection();
        Set<Integer> ids = new HashSet<>();
        DatabaseReference dbRef = new DatabaseReference(startElement);
        try
        {
            for(String query: new String[] {GENESYMBOL_QUERY, UNIPROT_QUERY})
            {
                try(PreparedStatement ps = connection.prepareStatement(query))
                {
                    ps.setString(1, dbRef.getAc());
                    try(ResultSet rs = ps.executeQuery())
                    {
                        while(rs.next())
                        {
                            ids.add(rs.getInt(2));
                        }
                    }
                }
            }
            Set<Element> databaseReferences = new HashSet<>();
            try(PreparedStatement ps = connection.prepareStatement(TERM_QUERY))
            {
                for(Integer id: ids)
                {
                    ps.setInt(1, id);
                    try(ResultSet rs = ps.executeQuery())
                    {
                        while(rs.next())
                        {
                            Element element = new Element("stub/GO//" + rs.getString(1));
                            databaseReferences.add(element);
                        }
                    }
                }
            }
            return databaseReferences.toArray(new Element[databaseReferences.size()]);
        }
        catch(SQLException e)
        {
            throw new BioHubFetchException( e, this );
        }
    }

    protected ThreadLocal<Connection> connection = new ThreadLocal<>();
    protected Connection getConnection()
    {
        try
        {
            if( connection.get() == null )
            {
                DataElementPath path = getModulePath();
                if(path != null)
                {
                    connection.set(SqlConnectionPool.getConnection(path.getDataCollection()));
                } else
                {
                    connection.set(SqlConnectionPool.getPersistentConnection(properties));
                }
            }
        }
        catch( Exception e )
        {
            if( log != null )
                log.log(Level.SEVERE, "Connection error", e);
        }
        return connection.get();
    }
}

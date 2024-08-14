package biouml.plugins.ensembl.biohub;

import java.sql.Connection;
import java.util.Properties;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.journal.ProjectUtils;

public abstract class AbstractEnsemblHub extends BioHubSupport
{
    protected Logger log = Logger.getLogger(AbstractEnsemblHub.class.getName());
    
    public static final String ENSEMBL_PATH_PROPERTY = "EnsemblPath";

    public AbstractEnsemblHub(Properties properties)
    {
        super(properties);
    }

    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        return 0;
    }

    @Override
    public Element[] getReference(Element startElement, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        return null;
    }

    protected synchronized Connection getConnection(DataElementPath path) throws BiosoftSQLException
    {
        Properties properties = path.getDataCollection().getInfo().getProperties();
        Connection persistentConnection = SqlConnectionPool.getPersistentConnection(
                properties.getProperty(SqlDataCollection.JDBC_URL_PROPERTY), properties.getProperty(SqlDataCollection.JDBC_USER_PROPERTY),
                properties.getProperty(SqlDataCollection.JDBC_PASSWORD_PROPERTY));
        SqlUtil.checkConnection(persistentConnection);
        return persistentConnection;
    }

    protected String getSpecies(Properties properties) throws Exception
    {
        String speciesName = properties.getProperty( SPECIES_PROPERTY );
        if( speciesName == null )
            throw new Exception("No species specified");
        return speciesName;
    }
    
    protected DataElementPath getEnsemblPath(Properties properties) throws Exception
    {
        if(properties.containsKey( ENSEMBL_PATH_PROPERTY ))
            return DataElementPath.create( properties.getProperty( ENSEMBL_PATH_PROPERTY ) );
        String speciesName = getSpecies( properties );
        DataElementPath projectPath = properties.containsKey( PROJECT_PROPERTY )
                ? DataElementPath.create( properties.getProperty( PROJECT_PROPERTY ) ) : null;
        DataElementPath ensemblPath = ProjectUtils.getPreferredDatabasePaths( projectPath ).get( "Ensembl (" + speciesName + ")" );
        if(ensemblPath == null)
            throw new Exception("No Ensembl database for " + speciesName);
        return ensemblPath;
    }

    protected static int getNumberOfQueryPlaceholders(String query)
    {
        int count = 0;
        int lastIndex = 0;

        while( lastIndex != -1 )
        {

            lastIndex = query.indexOf('?', lastIndex);

            if( lastIndex != -1 )
            {
                lastIndex++;
                count++;
            }
        }
        return count;
    }

}

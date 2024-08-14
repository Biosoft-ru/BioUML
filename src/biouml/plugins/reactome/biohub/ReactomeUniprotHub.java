package biouml.plugins.reactome.biohub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.util.logging.Logger;

import biouml.plugins.ensembl.tabletype.UniprotProteinTableType;
import biouml.plugins.reactome.ReactomeProteinTableType;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

/**
 * @author anna
 *
 */
public class ReactomeUniprotHub extends BioHubSupport
{
    private final Logger log = Logger.getLogger(ReactomeUniprotHub.class.getName());

    private final ReferenceType[] types = ReferenceTypeRegistry.getReferenceTypes(UniprotProteinTableType.class,
            ReactomeProteinTableType.class);

    private final ThreadLocal<Connection> connection = new ThreadLocal<>();

    private String internalDBUniprotId = null;

    private final Map<String, Integer> internalSpeciesId = new HashMap<>();
    private Boolean tableWorking = null;
    /**
     * @param properties
     */
    public ReactomeUniprotHub(Properties properties)
    {
        super(properties);
        // TODO Auto-generated constructor stub
    }

    public ReactomeUniprotHub()
    {
        super(getDefaultProperties());
    }

    private static Properties getDefaultProperties()
    {
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, "ReactomeUniprotHub");
        properties.put(SqlDataCollection.JDBC_URL_PROPERTY, "reactome");
        return properties;
    }

    /* (non-Javadoc)
     * @see ru.biosoft.access.biohub.BioHub#getPriority(ru.biosoft.access.biohub.TargetOptions)
     */
    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        return 0;
    }

    /* (non-Javadoc)
     * @see ru.biosoft.access.biohub.BioHub#getReference(ru.biosoft.access.biohub.Element, ru.biosoft.access.biohub.TargetOptions, java.lang.String[], int, int)
     */
    @Override
    public Element[] getReference(Element startElement, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        return null;
    }

    @Override
    public Map<String, String[]> getReferences(String[] inputList, ReferenceType inputType, ReferenceType outputType,
            Properties properties, FunctionJobControl jobControl)
    {
        try
        {
            String speciesName = properties.getProperty( SPECIES_PROPERTY );
            if( speciesName == null )
                throw new Exception("No species specified");
            Species species = Species.getSpecies(speciesName);
            if( species == null )
                throw new Exception("Species not found: " + speciesName);
            int speciesId = getSpeciesDatabaseId(species.getLatinName());
            if( speciesId == -1 )
                throw new Exception("Species not found in Reactome database: " + speciesName);
            String refDBId = getUniprotDatabaseId();
            if( refDBId == null )
                throw new Exception("Uniprot database is not supported in current Reactome database");

            try
            {
                generateTable( refDBId );
            }
            catch( Exception e )
            {
                //if index can not be build, do not use it
            }
            
            Connection conn = getConnection();
            String query = getQuery( inputType, outputType, speciesId );
            if( query == null )
                throw new Exception("Unsupported input/output type combination");

            try( PreparedStatement ps = conn.prepareStatement( query ) )
            {
                Map<String, String[]> result = new HashMap<>();
                for( int i = 0; i < inputList.length; i++ )
                {
                    List<String> curList = new ArrayList<>();
                    ps.setString( 1, inputList[i] );
                    try( ResultSet rs = ps.executeQuery() )
                    {
                        while( rs.next() )
                        {
                            curList.add( rs.getString( 1 ) );
                        }
                    }
                    result.put( inputList[i], curList.toArray( new String[0] ) );
                    if( jobControl != null )
                    {
                        jobControl.setPreparedness( i * 100 / inputList.length );
                        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                            return null;
                    }
                }
                return result;
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage(), e);
            if( jobControl != null )
                jobControl.functionTerminatedByError(e);
            return null;
        }
    }

    @Override
    public double getMatchingQuality(ReferenceType inputType, ReferenceType outputType)
    {
        if( inputType.equals(outputType) )
            return 0;
        if( inputType.getClass().equals(ReactomeProteinTableType.class) )
            return 1;
        if( inputType.getClass().equals(UniprotProteinTableType.class) && outputType.getClass().equals(ReactomeProteinTableType.class) )
            return 1;
        return 0;
    }

    @Override
    public ReferenceType[] getSupportedInputTypes()
    {
        return types.clone();
    }

    @Override
    public ReferenceType[] getSupportedMatching(ReferenceType inputType)
    {
        for( ReferenceType type : types )
        {
            if( inputType.equals(type) )
                return types.clone();
        }
        return null;
    }

    private String getQuery(ReferenceType inputType, ReferenceType outputType, int specieId)
    {
        if( isMatchingAccessible( inputType, outputType ) )
            return "SELECT identifier,siID FROM " + getUniprotMatchingTableName() + " WHERE species=" + specieId + " AND siID=?";
        else if( isMatchingAccessible( outputType, inputType ) )
            return "SELECT siID,identifier FROM " + getUniprotMatchingTableName() + " WHERE species=" + specieId + " AND identifier=?";
        return null;
    }

    private boolean isMatchingAccessible(ReferenceType inputType, ReferenceType outputType)
    {
        Class<? extends ReferenceType> inputClass = inputType.getClass();
        Class<? extends ReferenceType> outputClass = outputType.getClass();
        return ( inputClass.equals( ReactomeProteinTableType.class ) && outputClass.equals( UniprotProteinTableType.class ) );
    }

    private synchronized Connection getConnection()
    {
        try
        {
            if( connection.get() == null )
            {
                connection.set(SqlConnectionPool.getConnection(getModulePath().getDataCollection()));
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Connection error", e);
        }
        return connection.get();
    }

    private String getUniprotDatabaseId() throws Exception
    {
        if( internalDBUniprotId == null )
        {
            Connection conn = getConnection();
            try( Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery(
                            "SELECT DISTINCT DB_ID FROM ReferenceDatabase_2_name WHERE name like \"UniProt%\" AND name_rank=0" ); )
            {
                if( rs.next() )
                {
                    internalDBUniprotId = rs.getString(1);
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        return internalDBUniprotId;
    }

    private int getSpeciesDatabaseId(String speciesName) throws Exception
    {
        if( !internalSpeciesId.containsKey(speciesName) )
        {
            Connection conn = getConnection();
            try (PreparedStatement ps = conn
                    .prepareStatement( "SELECT DISTINCT DB_ID FROM DatabaseObject WHERE _displayName=? AND _class='Species'" ))
            {
                ps.setString(1, speciesName);
                try (ResultSet rs = ps.executeQuery())
                {
                    if( rs.next() )
                    {
                        int speciesId = rs.getInt( 1 );
                        internalSpeciesId.put( speciesName, speciesId );
                    }
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        Integer result = internalSpeciesId.get(speciesName);
        return result == null?-1:result;
    }
    
    private synchronized void generateTable(String refDBId) throws Exception
    {
        String tableName = "ReferenceEntity";
        if( tableWorking == null )
        {
            Connection conn = getConnection();
            try( Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery( "SHOW INDEXES FROM " + tableName + " WHERE Key_name LIKE 'biouml_%'" ) )
            {
                if(rs.next())
                {
                    tableWorking = true;
                }
                else
                {
                    st.execute( "CREATE INDEX biouml_identifier_index ON " + tableName + "(identifier(20))" );
                    tableWorking = true;
                }
            }
            catch( SQLException e )
            {
                tableWorking = false;
            }
            try
            {
                try
                {
                    SqlUtil.execute( conn, "SELECT identidier,species,siID FROM " + getUniprotMatchingTableName() + " LIMIT 1" );
                    tableWorking = true;
                }
                catch( BiosoftSQLException e )
                {
                    try( Statement st = conn.createStatement() )
                    {
                        st.execute(
                                "CREATE TABLE " + getUniprotMatchingTableName()
                                    + " SELECT re.identifier,gee.species,si.identifier as siID "
                                    + "FROM ReferenceEntity re JOIN EntityWithAccessionedSequence eas ON re.DB_ID=eas.referenceEntity "
                                    + "JOIN GenomeEncodedEntity gee ON (eas.DB_ID = gee.DB_ID) "
                                    + "JOIN DatabaseObject dbo ON (eas.DB_ID = dbo.DB_ID) "
                                    + "JOIN StableIdentifier si ON(dbo.stableIdentifier=si.DB_ID) " + "WHERE re.referenceDatabase='"
                                    + refDBId + "'" );
                    }
                    createMatchingIndexes( conn );
                    tableWorking = true;
                }
            }
            catch( SQLException e )
            {
                tableWorking = false;
            }
        }
        if( tableWorking == true )
            return;
        tableWorking = false;
    }

    private void createMatchingIndexes(Connection conn) throws SQLException
    {
        try( Statement st = conn.createStatement() )
        {
            st.execute( "CREATE INDEX from_reactome ON " + getUniprotMatchingTableName() + "(species,siID(20))" );
            st.execute( "CREATE INDEX to_reactome ON " + getUniprotMatchingTableName() + "(species,identifier(20))" );
        }
    }

    private String getUniprotMatchingTableName()
    {
        return "BioUML_uniprot_matching";
    }

}

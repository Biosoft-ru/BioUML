package ru.biosoft.access.biohub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.standard.type.Species;
import one.util.streamex.StreamEx;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.exception.BioHubFetchException;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

/** Table structure:
CREATE TABLE `hub` (
  `input` varchar(20) NOT NULL,
  `input_type` varchar(30) NOT NULL,
  `output` varchar(20) NOT NULL,
  `output_type` varchar(30) NOT NULL,
  `specie` varchar(50) NOT NULL,
  KEY `input` (`input`,`specie`,`output_type`),
  KEY `output` (`output`,`specie`,`input_type`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1

Alternative structure (semi-normalized):
CREATE TABLE `hub` (
  `input` varchar(20) NOT NULL,
  `input_type` int NOT NULL,
  `output` varchar(20) NOT NULL,
  `output_type` int NOT NULL,
  `specie` int NOT NULL,
  KEY `input` (`input`,`specie`,`output_type`),
  KEY `output` (`output`,`specie`,`input_type`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

CREATE TABLE `hub_terms` (
  `id` int primary key,
  `term` varchar(100) not null,
  key `term` (`term`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

 */
public abstract class SQLBasedHub extends BioHubSupport implements SqlConnectionHolder
{
    protected Logger log = Logger.getLogger( SQLBasedHub.class.getName() );
    private static final String MODULE_PATH = "modulePath";

    protected class Matching
    {
        ReferenceType inputType;
        ReferenceType outputType;
        boolean forward;
        double quality;

        public Matching(Class<? extends ReferenceType> inputType, Class<? extends ReferenceType> outputType, boolean forward, double quality)
        {
            super();
            this.inputType = ReferenceTypeRegistry.getReferenceType( inputType );
            this.outputType = ReferenceTypeRegistry.getReferenceType( outputType );
            this.forward = forward;
            this.quality = quality;
        }

        public String getQuery(String species)
        {
            String query;
            if( forward )
            {
                if( isNormalized() )
                {
                    query = "SELECT output FROM hub JOIN hub_terms t1 ON(input_type=t1.id) " + "JOIN hub_terms t2 ON(output_type=t2.id) "
                            + "JOIN hub_terms t3 ON(specie=t3.id) " + "WHERE input=? AND t1.term='" + inputType.getStableName()
                            + "' AND t2.term='" + outputType.getStableName() + "'";
                }
                else
                {
                    query = "SELECT output FROM hub WHERE input=? AND input_type='" + inputType.getStableName() + "' AND output_type='"
                            + outputType.getStableName() + "'";
                }
            }
            else
            {
                if( isNormalized() )
                {
                    query = "SELECT input FROM hub JOIN hub_terms t1 ON(input_type=t1.id) " + "JOIN hub_terms t2 ON(output_type=t2.id) "
                            + "JOIN hub_terms t3 ON(specie=t3.id) " + "WHERE output=? AND t2.term='" + inputType.getStableName()
                            + "' AND t1.term='" + outputType.getStableName() + "'";
                }
                else
                {
                    query = "SELECT input FROM hub WHERE output=? AND output_type='" + inputType.getStableName() + "' AND input_type='"
                            + outputType.getStableName() + "'";
                }
            }
            if( controlSpecies() && species != null && !species.equals( Species.ANY_SPECIES ) )
            {
                if( isNormalized() )
                {
                    query += " AND t3.term='" + species + "'";
                }
                else
                {
                    query += " AND specie='" + species + "'";
                }
            }
            return query;
        }

        private String[] species;
        public synchronized StreamEx<String> supportedSpecies()
        {
            if( species == null )
            {
                Connection connection = null;
                connection = getConnection();
                if( connection == null )
                    return StreamEx.empty();
                String query;
                if( forward )
                {
                    if( isNormalized() )
                    {
                        query = "SELECT DISTINCT t3.term FROM hub JOIN hub_terms t1 ON(input_type=t1.id) "
                                + "JOIN hub_terms t2 ON(output_type=t2.id) " + "JOIN hub_terms t3 ON(specie=t3.id) WHERE t1.term='"
                                + inputType.getStableName() + "' AND t2.term='" + outputType.getStableName() + "'";
                    }
                    else
                    {
                        query = "SELECT DISTINCT specie FROM hub WHERE input_type='" + inputType.getStableName() + "' AND output_type='"
                                + outputType.getStableName() + "'";
                    }
                }
                else
                {
                    if( isNormalized() )
                    {
                        query = "SELECT DISTINCT t3.term FROM hub JOIN hub_terms t1 ON(input_type=t1.id) "
                                + "JOIN hub_terms t2 ON(output_type=t2.id) " + "JOIN hub_terms t3 ON(specie=t3.id) WHERE t2.term='"
                                + inputType.getStableName() + "' AND t1.term='" + outputType.getStableName() + "'";
                    }
                    else
                    {
                        query = "SELECT DISTINCT specie FROM hub WHERE output_type='" + inputType.getStableName() + "' AND input_type='"
                                + outputType.getStableName() + "'";
                    }
                }
                List<String> result;
                try
                {
                    result = SqlUtil.queryStrings( connection, query );
                    if( !result.isEmpty() )
                        result.add( Species.ANY_SPECIES );
                    result.retainAll( Species.SPECIES_PATH.getDataCollection().getNameList() );
                }
                catch( BiosoftSQLException e )
                {
                    e.log();
                    result = Collections.emptyList();
                }
                species = result.toArray( new String[0] );
            }
            return StreamEx.of( species );
        }

        public boolean isValid()
        {
            return inputType != null && outputType != null;
        }

        boolean isSpeciesMatches(Properties input)
        {
            return !controlSpecies() || !input.containsKey( SPECIES_PROPERTY )
                    || supportedSpecies().has( input.getProperty( SPECIES_PROPERTY ) );
        }

        boolean isInputMatches(Properties input)
        {
            return inputType.toString().equals( input.getProperty( TYPE_PROPERTY ) );
        }

        boolean isOutputMatches(Properties output)
        {
            return outputType.toString().equals( output.getProperty( TYPE_PROPERTY ) );
        }
    }

    protected abstract Matching[] getMatchings();

    protected boolean valid = true;
    protected Boolean normalized;

    public SQLBasedHub(Properties properties)
    {
        super( properties );
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

    @Override
    public Map<String, String[]> getReferences(String[] inputList, ReferenceType inputType, ReferenceType outputType,
            Properties properties, FunctionJobControl jobControl)
    {
        Matching matching = StreamEx.of( getMatchings() ).findFirst( m -> m.inputType == inputType && m.outputType == outputType )
                .orElse( null );
        if( matching == null )
            return null;
        try (PreparedStatement ps = getConnection().prepareStatement( matching.getQuery( properties.getProperty( SPECIES_PROPERTY ) ) ))
        {
            Map<String, String[]> result = new HashMap<>();
            int i = 0;
            for( String inputId : inputList )
            {
                String processedId = processInputId( inputId, inputType );
                ps.setString( 1, processedId );
                try(ResultSet rs = ps.executeQuery())
                {
                    List<String> values = new ArrayList<>();
                    while( rs.next() )
                    {
                        values.add( rs.getString( 1 ) );
                    }
                    result.put( inputId, values.toArray( new String[0] ) );
                }
                i++;
                if( jobControl != null )
                {
                    if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                        return null;
                    jobControl.setPreparedness( (int) ( ( i * 100.0 ) / inputList.length ) );
                }
            }
            return result;
        }
        catch( SQLException e )
        {
            throw new BioHubFetchException( e, this );
        }
    }

    protected String processInputId(String tfId, ReferenceType inputType)
    {
        return tfId;
    }

    protected boolean isNormalized()
    {
        if( normalized == null )
        {
            normalized = SqlUtil.hasTable( getConnection(), "hub_terms" );
        }
        return normalized;
    }

    @Override
    public Connection getConnection()
    {
        if( !valid )
            return null;
        try
        {
            String path = properties.getProperty( MODULE_PATH );
            if( path == null )
                path = properties.getProperty( DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY );
            Connection connection = null;
            if( path != null )
            {
                DataCollection<?> module = CollectionFactory.getDataCollection( path );
                if( module == null )
                {
                    log.log(Level.SEVERE,  getName() + ": no module found (" + path + "); hub is disabled" );
                    valid = false;
                    return null;
                }
                connection = SqlConnectionPool.getConnection( module );
            }
            if( connection == null )
            {
                if( properties.containsKey( SqlDataCollection.JDBC_URL_PROPERTY ) )
                {
                    try
                    {
                        connection = SqlConnectionPool.getPersistentConnection( properties );
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE,  getName() + ": connection error; hub is disabled" );
                        valid = false;
                        return null;
                    }
                }
                else
                {
                    log.log(Level.SEVERE,  getName() + ": no connection properties found; hub is disabled" );
                    valid = false;
                    return null;
                }
            }
            SqlUtil.checkConnection( connection );
            return connection;
        }
        catch( Exception e )
        {
            ExceptionRegistry.log( e );
        }
        return null;
    }

    @Override
    public Properties[] getSupportedInputs()
    {
        return StreamEx.of( getMatchings() ).cross( Matching::supportedSpecies ).mapKeys( matching -> matching.inputType ).distinct()
                .invert().mapKeyValue( SQLBasedHub::createProperties ).toArray( Properties[]::new );
    }

    @Override
    public Properties[] getSupportedMatching(Properties input)
    {
        List<Properties> result = new ArrayList<>();
        for( Matching matching : getMatchings() )
        {
            if( matching.isValid() && matching.isInputMatches( input ) && matching.isSpeciesMatches( input ) )
            {
                Properties property = (Properties)input.clone();
                property.setProperty( TYPE_PROPERTY, matching.outputType.toString() );
                result.add( property );
            }
        }
        return result.toArray( new Properties[0] );
    }

    @Override
    public double getMatchingQuality(Properties input, Properties output)
    {
        if( controlSpecies() && input.containsKey( SPECIES_PROPERTY )
                && !input.getProperty( SPECIES_PROPERTY ).equals( output.getProperty( SPECIES_PROPERTY ) ) )
            return 0;
        for( Matching matching : getMatchings() )
        {
            if( matching.isValid() && matching.isInputMatches( input ) && matching.isOutputMatches( output ) )
            {
                if( matching.isSpeciesMatches( input ) )
                {
                    return matching.quality;
                }
                return 0;
            }
        }
        return 0;
    }

    protected boolean controlSpecies()
    {
        return true;
    }

    public static void createHubTable(Connection con)
    {
        SqlUtil.execute( con, "DROP TABLE IF EXISTS `hub`" );
        SqlUtil.execute( con,
            "CREATE TABLE `hub` ("
          + "`input` varchar(25) NOT NULL,"
          + "`input_type` varchar(30) NOT NULL,"
          + "`output` varchar(20) NOT NULL,"
          + "`output_type` varchar(30) NOT NULL,"
          + "`specie` varchar(50) NOT NULL,"
          + "KEY `input` (`input`,`specie`,`output_type`),"
          + "KEY `output` (`output`,`specie`,`input_type`)"
          + ") ENGINE=MyISAM DEFAULT CHARSET=latin1");
    }
}

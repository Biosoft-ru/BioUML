package biouml.plugins.ensembl.biohub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.exception.BioHubFetchException;

import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

public abstract class EnsemblToExternalHub extends AbstractEnsemblExternalHub
{
    protected static final String RESULT_COLUMN = "#RESULT_COLUMN#";

    //Property to indicate whether collection has ID with suffix, not to trim in matching query. For example, "AT3G04170.1"
    //Should be used in config separately for each object type: hasSuffix.Translation=true
    public static final String ID_HAS_SUFFIX = "hasSuffix";

    protected ReferenceType[] supportedTypes = null;

    public EnsemblToExternalHub(Properties properties)
    {
        super(properties);
    }

    protected void initSupportedTypes()
    {
        if( supportedTypes == null )
        {
            supportedTypes = new ReferenceType[getSupportedTypeRecords().length];
            for( int i = 0; i < getSupportedTypeRecords().length; i++ )
            {
                supportedTypes[i] = getSupportedTypeRecords()[i].getType();
            }
        }
    }

    @Override
    public ReferenceType[] getSupportedInputTypes()
    {
        initSupportedTypes();
        return new ReferenceType[] {getInputType()};
    }

    protected TypeRecord getTypeRecord(ReferenceType inputType, ReferenceType outputType) throws Exception
    {
        initSupportedTypes();
        if( !inputType.equals(getInputType()) )
            throw new Exception("Invalid input type");
        for( TypeRecord curRecord : getSupportedTypeRecords() )
        {
            if( curRecord.getType() != null && curRecord.getType().equals(outputType) )
            {
                return curRecord;
            }
        }
        throw new Exception("Invalid output type");
    }

    @Override
    public Properties[] getSupportedMatching(Properties input)
    {
        if( getInputType() == null )
            return EMPTY_PROPERTIES;
        initSupportedTypes();
        ReferenceType inputType = ReferenceTypeRegistry.getReferenceType(input.getProperty(TYPE_PROPERTY));
        if( inputType.equals(getInputType()) )
        {
            try
            {
                Set<TypeRecord> enabledTypes = getEnabledTypes(getEnsemblPath(input));
                List<Properties> result = new ArrayList<>();
                for( TypeRecord type : enabledTypes )
                {
                    if( type.getType() == null )
                        continue;
                    Properties properties = (Properties)input.clone();
                    properties.setProperty(BioHub.TYPE_PROPERTY, type.getType().toString());
                    result.add(properties);
                }
                return result.toArray(new Properties[result.size()]);
            }
            catch( Exception e )
            {
            }
        }
        return EMPTY_PROPERTIES;
    }

    @Override
    public double getMatchingQuality(Properties input, Properties output)
    {
        try
        {
            if( !input.getProperty( SPECIES_PROPERTY ).equals( output.getProperty( SPECIES_PROPERTY ) ) )
                return 0;
            ReferenceType inputType = ReferenceTypeRegistry.getReferenceType(input.getProperty(TYPE_PROPERTY));
            ReferenceType outputType = ReferenceTypeRegistry.getReferenceType(output.getProperty(TYPE_PROPERTY));
            return getTypeRecord(inputType, outputType).getQuality();
        }
        catch( Exception e )
        {
            return 0;
        }
    }

    @Override
    public Map<String, String[]> getReferences(String[] inputList, ReferenceType inputType, ReferenceType outputType,
            Properties properties, FunctionJobControl jobControl)
    {
        try
        {
            TypeRecord record = getTypeRecord(inputType, outputType);
            
            String species = getSpecies( properties );
            DataElementPath ensemblPath = getEnsemblPath(properties);
            Connection conn = getConnection(ensemblPath);
            String query = getQueryTemplate(ensemblPath).replaceAll(RESULT_COLUMN, record.getXrefColumnName())
                    + record.getDbRestrictionClause(species);
            boolean hasSuffix = hasSuffix( ensemblPath );
            try (PreparedStatement ps = conn.prepareStatement( query ))
            {
                Map<String, String[]> result = new HashMap<>();
                List<String> curList = new ArrayList<>();
                int n = getNumberOfQueryPlaceholders( query );
                for( int i = 0; i < inputList.length; i++ )
                {
                    curList.clear();
                    String inputId = record.strip( hasSuffix ? inputList[i] : inputList[i].replaceFirst( "\\.\\d++", "" ) );
                    for( int j = 1; j <= n; j++ )
                        ps.setString( j, inputId );
                    try (ResultSet rs = ps.executeQuery())
                    {
                        while( rs.next() )
                        {
                            curList.add( rs.getString( 1 ) );
                        }
                    }
                    result.put( inputList[i], curList.toArray( new String[curList.size()] ) );
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
            if( jobControl != null )
                jobControl.functionTerminatedByError(e);
            throw new BioHubFetchException( e, this );
        }
    }

    protected abstract String getQueryTemplate(DataElementPath ensemblPath);

    protected abstract ReferenceType getInputType();

    /**
     * Check whether matching object type has suffixes in sql table
     * Do not trim suffix in identifier if it is present in sql
     * Use config property for each object type separately. For example: hasSuffix.Transcript=true
     */
    protected boolean hasSuffix(DataElementPath ensemblPath)
    {
        try
        {
            return Boolean.parseBoolean(
                    ensemblPath.getDataCollection().getInfo().getProperties().getProperty( ID_HAS_SUFFIX + "." + getObjectType() ) );
        }
        catch( Exception ex )
        {
            return false;
        }
    }
}

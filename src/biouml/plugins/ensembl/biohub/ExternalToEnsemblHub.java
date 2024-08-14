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

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.exception.BioHubFetchException;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

public abstract class ExternalToEnsemblHub extends AbstractEnsemblExternalHub
{
    protected ReferenceType[] supportedTypes = null;

    public ExternalToEnsemblHub(Properties properties)
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
        return supportedTypes.clone();
    }

    protected TypeRecord getTypeRecord(ReferenceType inputType, ReferenceType outputType) throws Exception
    {
        initSupportedTypes();
        if( !outputType.equals(getOutputType()) )
            throw new Exception("Invalid output type");
        for( TypeRecord curRecord : getSupportedTypeRecords() )
        {
            if( curRecord.getType() != null && curRecord.getType().equals(inputType) )
            {
                return curRecord;
            }
        }
        throw new Exception("Invalid input type");
    }

    @Override
    public Properties[] getSupportedMatching(Properties input)
    {
        if( getOutputType() == null )
            return EMPTY_PROPERTIES;
        initSupportedTypes();
        String referenceTypeName = input.getProperty(TYPE_PROPERTY);
        if( referenceTypeName == null )
            return EMPTY_PROPERTIES;
        ReferenceType inputType = ReferenceTypeRegistry.getReferenceType(referenceTypeName);
        List<Properties> result = new ArrayList<>();
        Set<TypeRecord> enabledTypes;
        try
        {
            enabledTypes = getEnabledTypes(getEnsemblPath(input));
        }
        catch( Exception e )
        {
            return EMPTY_PROPERTIES;
        }
        for( TypeRecord supportedType : enabledTypes )
        {
            if( supportedType.getType() != null && inputType.equals(supportedType.getType()) )
            {
                Properties properties = (Properties)input.clone();
                properties.setProperty(TYPE_PROPERTY, getOutputType().toString());
                result.add(properties);
            }
        }
        return result.toArray(new Properties[result.size()]);
    }

    @Override
    public ReferenceType[] getSupportedMatching(ReferenceType inputType)
    {
        return null;
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
            Set<TypeRecord> enabledTypes = getEnabledTypes(getEnsemblPath(input));
            TypeRecord typeRecord = getTypeRecord(inputType, outputType);
            if( !enabledTypes.contains(typeRecord) )
                return 0;
            return typeRecord.getQuality();
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
        PreparedStatement ps = null;
        try
        {
            TypeRecord record = getTypeRecord(inputType, outputType);
            String species = getSpecies( properties );
            DataElementPath ensemblPath = getEnsemblPath(properties);
            DataCollection<DataElement> chrCollection = TrackUtils.getPrimarySequencesPath( ensemblPath ).getDataCollection();
            Connection conn = getConnection(ensemblPath);
            String query = getQueryTemplate(species, ensemblPath, record);
            ps = conn.prepareStatement(query);
            Map<String, String[]> result = new HashMap<>();
            List<String> curList = new ArrayList<>();
            int n = getNumberOfQueryPlaceholders(query);
            for( int i = 0; i < inputList.length; i++ )
            {
                curList.clear();
                for( int j = 1; j <= n; j++ )
                    ps.setString(j, record.strip(inputList[i]));
                try (ResultSet rs = ps.executeQuery())
                {
                    while( rs.next() )
                    {
                        if( isChromosomeAcceptable( rs.getString( 2 ), chrCollection ) )
                            curList.add( rs.getString( 1 ) );
                    }
                    result.put( inputList[i], curList.toArray( new String[curList.size()] ) );
                    if( jobControl != null )
                    {
                        jobControl.setPreparedness( i * 100 / inputList.length );
                        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                            return null;
                    }
                }
            }
            return result;
        }
        catch( Exception e )
        {
            if( jobControl != null )
                jobControl.functionTerminatedByError(e);
            throw new BioHubFetchException( e, this );
        }
        finally
        {
            SqlUtil.close(ps, null);
        }
    }
    
    protected boolean isChromosomeAcceptable(String chr, DataCollection<?> chrCollection)
    {
        return chrCollection.contains( chr );
    }

    protected abstract String getQueryTemplate(String species, DataElementPath ensemblPath, TypeRecord typeRecord);

    protected abstract ReferenceType getOutputType();
}

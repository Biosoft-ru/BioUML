package ru.biosoft.access.biohub;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.IntSupplier;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import biouml.standard.type.Species;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.LazyValue;

/**
 * Default implementation of BioHub methods.
 */
public abstract class BioHubSupport implements BioHub
{
    private static final String SHORT_NAME_PROPERTY = "shortName";
    protected Properties properties = null;

    public BioHubSupport(Properties properties)
    {
        this.properties = properties;
    }

    @Override
    public Element[] getMinimalPath(Element element1, Element element2, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        throw new UnsupportedOperationException("Unsupported method getMinimalPath for BioHub: " + getClass().getName());
    }

    @Override
    public List<Element[]> getMinimalPaths(Element key, Element[] targets, TargetOptions dbOptions, String[] relationTypes, int maxLength,
            int direction)
    {
        return StreamEx.of( targets ).map( t -> getMinimalPath( key, t, dbOptions, relationTypes, maxLength, direction ) ).toList();
    }

    @Override
    public Map<Element, Element[]> getReferences(Element[] startElements, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        return StreamEx.of( startElements ).toMap( e -> getReference( e, dbOptions, relationTypes, maxLength, direction ) );
    }

    @Override
    public String getName()
    {
        return properties==null?null:properties.getProperty(DataCollectionConfigConstants.NAME_PROPERTY);
    }

    @Override
    public String getShortName()
    {
        String shortName = properties == null ? null : properties.getProperty(SHORT_NAME_PROPERTY);
        return shortName == null ? getName() : shortName;
    }

    @Override
    public DataElementPath getModulePath()
    {
        return DataElementPath.create( properties == null ? null : properties.getProperty( MODULE_NAME_PROPERTY ) );
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public static BioHub createInstance(String hubName)
    {
        return BioHubRegistry.getBioHub(hubName);
    }

    public ReferenceType[] getSupportedInputTypes()
    {
        return new ReferenceType[0];
    }

    private static LazyValue<String[]> allSpecies = new LazyValue<>( "All species",
            () -> Species.SPECIES_PATH.getDataCollection().names().toArray( String[]::new ) );

    protected static final Properties[] EMPTY_PROPERTIES = new Properties[0];
    /** Property for storing module name to be used for complete Element path*/
    protected static final String MODULE_NAME_PROPERTY = "moduleName";
    public String[] getSupportedSpecies()
    {
        return allSpecies.get();
    }

    @Override
    public Properties[] getSupportedInputs()
    {
        ReferenceType[] types = getSupportedInputTypes();
        if(types == null || types.length == 0) return EMPTY_PROPERTIES;
        String[] species = getSupportedSpecies();
        if(species == null || species.length == 0) return EMPTY_PROPERTIES;
        return StreamEx.of( species ).cross( types ).mapKeyValue( BioHubSupport::createProperties ).toArray( Properties[]::new );
    }

    public ReferenceType[] getSupportedMatching(ReferenceType inputType)
    {
        return new ReferenceType[0];
    }

    @Override
    public Properties[] getSupportedMatching(Properties input)
    {
        String referenceTypeName = input.getProperty(TYPE_PROPERTY);
        if(referenceTypeName == null) return EMPTY_PROPERTIES;
        ReferenceType inputType = ReferenceTypeRegistry.getReferenceType(referenceTypeName);
        ReferenceType[] matching = getSupportedMatching(inputType);
        if(matching == null || matching.length == 0) return EMPTY_PROPERTIES;
        return Stream.of(matching).map( m -> {
            Properties properties = (Properties)input.clone();
            properties.setProperty(TYPE_PROPERTY, m.toString());
            return properties;
        }).toArray( Properties[]::new );
    }

    public double getMatchingQuality(ReferenceType inputType, ReferenceType outputType)
    {
        return 0;
    }

    @Override
    public double getMatchingQuality(Properties input, Properties output)
    {
        if( input.containsKey( SPECIES_PROPERTY )
                && !input.getProperty( SPECIES_PROPERTY ).equals( output.getProperty( SPECIES_PROPERTY ) ) )
            return 0;
        return getMatchingQuality(ReferenceTypeRegistry.getReferenceType(input.getProperty(TYPE_PROPERTY)),
                ReferenceTypeRegistry.getReferenceType(output.getProperty(TYPE_PROPERTY)));
    }

    public Map<String, String[]> getReferences(String[] inputList, ReferenceType inputType, ReferenceType outputType,
            Properties properties, FunctionJobControl jobControl)
    {
        return null;
    }

    @Override
    public Map<String, String[]> getReferences(String[] inputList, Properties input, Properties output, FunctionJobControl jobControl)
    {
        if( input.containsKey( SPECIES_PROPERTY )
                && !input.getProperty( SPECIES_PROPERTY ).equals( output.getProperty( SPECIES_PROPERTY ) ) )
            return null;
        return getReferences(inputList, ReferenceTypeRegistry.getReferenceType(input.getProperty(TYPE_PROPERTY)),
                ReferenceTypeRegistry.getReferenceType(output.getProperty(TYPE_PROPERTY)), output, jobControl);
    }

    protected static int getPriority(TargetOptions dbOptions, @Nonnull DataElementPath path, IntSupplier defaultPriority)
    {
        DataElementPathSet dbList = dbOptions.getUsedCollectionPaths();
        if( dbList.size() > 0 )
        {
            for( DataElementPath cr : dbList )
            {
                if( !cr.isDescendantOf( path ) )
                {
                    return 0;
                }
            }
            return defaultPriority.getAsInt();
        }
        return 0;
    }

    public static Properties createProperties(Species species, ReferenceType type)
    {
        return createProperties( species.getLatinName(), type.toString() );
    }

    public static Properties createProperties(Species species, ReferenceType type, DataElementPath projectPath)
    {
        return createProperties( species.getLatinName(), type.toString(), projectPath.toString() );
    }

    public static Properties createProperties(String species, ReferenceType type)
    {
        return createProperties( species, type.toString() );
    }

    public static Properties createProperties(String species, String type)
    {
        Properties properties = new Properties();
        properties.setProperty( SPECIES_PROPERTY, species );
        properties.setProperty( TYPE_PROPERTY, type );
        return properties;
    }

    public static Properties createProperties(ReferenceType type)
    {
        Properties properties = new Properties();
        properties.setProperty( TYPE_PROPERTY, type.toString() );
        return properties;
    }

    public static Properties createProperties(String species, String type, String projectPath)
    {
        Properties properties = new Properties();
        properties.setProperty( SPECIES_PROPERTY, species );
        properties.setProperty( TYPE_PROPERTY, type );
        properties.setProperty( PROJECT_PROPERTY, projectPath );
        return properties;
    }

}

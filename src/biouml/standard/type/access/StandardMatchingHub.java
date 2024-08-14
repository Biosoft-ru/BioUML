package biouml.standard.type.access;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

import biouml.standard.type.Referrer;
import biouml.standard.type.Species;
import one.util.streamex.StreamEx;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.Cache;

public class StandardMatchingHub extends BioHubSupport
{
    protected static class MatchingData
    {
        Map<String, Set<String>> forward = new HashMap<>();
        Map<String, Set<String>> backward = new HashMap<>();

        public void accept(String from, String to)
        {
            forward.computeIfAbsent( from, k -> new HashSet<>() ).add( to );
            backward.computeIfAbsent( to, k -> new HashSet<>() ).add( from );
        }
    }

    protected final Supplier<MatchingData> cache = Cache.soft( this::createMatchingData );

    protected MatchingData createMatchingData()
    {
        MatchingData data = new MatchingData();
        String dbId = targetType.getMiriamId();
        StreamEx.of( hubCollection.getDataCollection().stream() ).select( Referrer.class ).mapToEntry( ref -> ref.getDatabaseReferences() )
                .nonNullValues()
                .flatMapValues( Arrays::stream ).filterValues( dbRef -> dbRef.getDatabaseName().equals( dbId ) )
                .forKeyValue( (de, dbRef) -> data.accept( de.getName(), dbRef.getAc() ) );
        return data;
    }

    protected ReferenceType sourceType;
    protected ReferenceType targetType;
    protected final DataElementPath hubCollection;
    protected Species species;

    public StandardMatchingHub(Properties properties)
    {
        super( properties );
        this.hubCollection = getModulePath().getRelativePath( properties.getProperty( "collection" ) );
        this.sourceType = ReferenceTypeRegistry.getReferenceType( hubCollection.getDataCollection().getInfo()
                .getProperty( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY ) );
        this.targetType = ReferenceTypeRegistry.getReferenceType( properties.getProperty( "target" ) );
        this.species = Species.getDefaultSpecies( getModulePath().getDataCollection() );
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
        MatchingData matchingData = cache.get();
        Map<String, Set<String>> data;
        if( inputType == sourceType && outputType == targetType )
            data = matchingData.forward;
        else if( inputType == targetType && outputType == sourceType )
            data = matchingData.backward;
        else
            return Collections.emptyMap();
        return StreamEx.of( inputList ).mapToEntry( data::get ).nonNullValues().mapValues( list -> list.toArray( new String[0] ) ).toMap();
    }

    @Override
    public Properties[] getSupportedInputs()
    {
        return StreamEx.of( sourceType, targetType ).map( type -> createProperties( species, type ) ).toArray( Properties[]::new );
    }

    @Override
    public Properties[] getSupportedMatching(Properties input)
    {
        String speciesName = input.getProperty( SPECIES_PROPERTY );
        if( speciesName != null && !speciesName.equals( Species.ANY_SPECIES ) && !species.getLatinName().equals( speciesName ) )
            return new Properties[0];
        String type = input.getProperty( TYPE_PROPERTY );
        if( type.equals( sourceType.toString() ) )
            return new Properties[] {createProperties( species, targetType )};
        else if( type.equals( targetType.toString() ) )
            return new Properties[] {createProperties( species, sourceType )};
        return new Properties[0];
    }

    @Override
    public double getMatchingQuality(Properties input, Properties output)
    {
        String speciesName = input.getProperty( SPECIES_PROPERTY );
        if( speciesName != null && !speciesName.equals( Species.ANY_SPECIES ) && !species.getLatinName().equals( speciesName ) )
            return 0;
        speciesName = output.getProperty( SPECIES_PROPERTY );
        if( speciesName != null && !speciesName.equals( Species.ANY_SPECIES ) && !species.getLatinName().equals( speciesName ) )
            return 0;
        String inputType = input.getProperty( TYPE_PROPERTY );
        String outputType = output.getProperty( TYPE_PROPERTY );
        if( ( inputType.equals( sourceType.toString() ) && outputType.equals( targetType.toString() ) )
                || ( inputType.equals( targetType.toString() ) && outputType.equals( sourceType.toString() ) ) )
            return 1;
        return 0;
    }
}

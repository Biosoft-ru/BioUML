package biouml.plugins.gtrd;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.bsa.BindingElement;
import ru.biosoft.bsa.TranscriptionFactor;
import ru.biosoft.bsa.analysis.FrequencyMatrix;

public class GTRDMatrix extends FrequencyMatrix
{
    public GTRDMatrix(DataCollection<?> origin, String name, FrequencyMatrix frequencyMatrix)
    {
        super(origin, name, frequencyMatrix);
        init(getOrigin());
        List<TranscriptionFactor> factors = Arrays.stream( classReferences.get( name ) )
            .map( cl->new TranscriptionFactor( cl, null, cl, ReferenceTypeRegistry.getReferenceType( "Proteins: GTRD" ), Species.ANY_SPECIES ) )
            .collect( Collectors.toList() );
        setBindingElement(new BindingElement(getBindingElementName(), factors));
    }

    private static Map<String, String[]> trackReferences;
    private static Map<String, Map<String, String[]>> speciesUniprotReferences;
    private static Map<String, String[]> classReferences;

    private synchronized static void init(DataCollection<?> origin)
    {
        if(classReferences != null) return;
        BioHub bioHub = BioHubRegistry.getBioHub("GTRD hub");
        if(!(bioHub instanceof GTRDHub))
        {
            throw new InternalException( "GTRD hub is not available!" );
        }
        GTRDHub hub = (GTRDHub)bioHub;
        Properties inputProperties = new Properties();
        Properties outputProperties = new Properties();
        String[] matrixIDs = origin.names().toArray( String[]::new );
        
        inputProperties.put(BioHub.TYPE_PROPERTY, "Site models: GTRD");
        outputProperties.put(BioHub.TYPE_PROPERTY, "ChIP-seq peaks: GTRD");
        trackReferences = hub.getReferences(matrixIDs, inputProperties, outputProperties, null);

        speciesUniprotReferences = new HashMap<>();
        for(Species species: Species.allSpecies())
        {
            String name = species.getLatinName();
            inputProperties.put(BioHub.SPECIES_PROPERTY, name);
            outputProperties.put(BioHub.TYPE_PROPERTY, "Proteins: UniProt");
            outputProperties.put(BioHub.SPECIES_PROPERTY, name);
            Map<String, String[]> uniprotReferences = BioHubRegistry.getReferences(matrixIDs, inputProperties, outputProperties, null);
            if(uniprotReferences != null)
                speciesUniprotReferences.put(name, uniprotReferences);
        }

        outputProperties.put(BioHub.TYPE_PROPERTY, "Proteins: GTRD");
        inputProperties.put( BioHub.SPECIES_PROPERTY, "Homo sapiens" );
        outputProperties.put( BioHub.SPECIES_PROPERTY, "Homo sapiens" );
        classReferences = BioHubRegistry.getReferences(matrixIDs, inputProperties, outputProperties, null);
    }

    public String getReferencesString()
    {
        return String.join(", ", getReferences());
    }

    public List<String> getReferences()
    {
        init(getOrigin());
        String[] references = trackReferences.get(getName());
        if( references == null )
            return Collections.emptyList();
        return Arrays.asList(references);
    }

    private List<String> getUniprotIDList()
    {
        init(getOrigin());
        String[] references = speciesUniprotReferences.get(Species.getDefaultSpecies(null).getLatinName()).get(getName());
        if( references == null )
            return Collections.emptyList();
        return Arrays.asList(references);
    }

    public String getUniprotIDs()
    {
        return String.join(", ", getUniprotIDList());
    }

    private List<String> getClassReferencesList()
    {
        init(getOrigin());
        String[] references = classReferences.get(getName());
        if( references == null )
            return Collections.emptyList();
        return Arrays.asList(references);
    }

    public String getClassReferences()
    {
        return String.join(", ", getClassReferencesList());
    }

    /**
     * @return first class
     */
    public String getClassReference()
    {
        List<String> classReferencesList = getClassReferencesList();
        return classReferencesList.isEmpty() ? "" : classReferencesList.get(0);
    }
}

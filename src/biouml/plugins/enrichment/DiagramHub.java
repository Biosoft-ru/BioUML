package biouml.plugins.enrichment;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.DynamicProperty;

import one.util.streamex.EntryStream;
import one.util.streamex.MoreCollectors;
import one.util.streamex.StreamEx;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;

/**
 * @author lan
 *
 */
public class DiagramHub extends BioHubSupport
{
    private final DataElementPath parentPath;
    private final DataElementPath referenceCollection;
    private final String type;
    public static final String DIAGRAM_HUB_NAME = "Diagrams collection";

    public DiagramHub(Properties properties, DataElementPath collectionPath, DataElementPath referenceCollection, String type)
    {
        super(properties);
        this.parentPath = collectionPath;
        this.type = type;
        this.referenceCollection = referenceCollection;
    }

    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        return 1;
    }

    @Override
    public Element[] getReference(Element startElement, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        Map<Element, Element[]> references = getReferences(new Element[] {startElement}, dbOptions, relationTypes, maxLength, direction);
        return references == null?null:references.get(startElement);
    }
    
    private StreamEx<ru.biosoft.access.core.DataElementPath> resolveKernels(Node node)
    {
        if(node == null)
            return null;
        Object entityType = node.getAttributes().getValue( "sbgn:entityType" );
        if(entityType != null)
        {
            if(type.equals( "Proteins" ) ^ entityType.equals( "simple chemical" ))
                return null;
        }
        Base kernel = node.getKernel();
        if(kernel instanceof Stub)
        {
            Object path = kernel.getAttributes().getValue( "completeName" );
            if(path instanceof String)
                return StreamEx.of(DataElementPath.create((String)path));
            return null;
        }
        if(kernel.getOrigin() == null)
        {
            Object path = kernel.getAttributes().getValue( Util.ORIGINAL_PATH );
            if(path instanceof String)
                return StreamEx.of(DataElementPath.create((String)path));
        }
        return StreamEx.of(kernel.getCompletePath());
    }
    
    public Map<String, DataElementPath> getKernelMap()
    {
        return parentPath.getDataCollection().stream( Diagram.class )
            .flatMap( dgr -> dgr.stream(Node.class))
                .flatMap( node -> resolveKernels( node ) )
                .collect( Collectors.toMap( DataElementPath::getName, Function.identity(), (a, b) -> a ) );
    }
    
    @Override
    public Map<Element, Element[]> getReferences(Element[] startElements, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        Set<String> refNames = null;
        if(referenceCollection != null)
        {
            refNames = referenceCollection.getDataCollection().names().collect( Collectors.toSet() );
        }
        Map<Element, Set<String>> map = StreamEx.of( parentPath.getDataCollection().stream( Diagram.class ) )
            .mapToEntry(
                dgr -> new Element(dgr.getCompletePath()),
                dgr -> dgr.stream( Node.class )
                    .flatMap(node -> resolveKernels( node ))
                    .map( kernel -> kernel.getName() )
                    .toSet() )
            .toMap();
        Map<String, Element[]> revMap = EntryStream.of( map ).invert().flatMapKeys( Set::stream )
                .grouping( MoreCollectors.toArray( Element[]::new ) );
        if(refNames != null)
            revMap.keySet().retainAll( refNames );
        else
            refNames = revMap.keySet();
        Map<Element, Element[]> result = StreamEx.of(startElements).mapToEntry( e -> revMap.get( e.getAccession() ) )
            .nonNullValues().toMap();
        boolean hitsMode = StreamEx.of(dbOptions.getUsedCollectionPaths()).map(Object::toString).has(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_HITS_RECORD);
        if(hitsMode)
        {
            int inputCount = (int)StreamEx.of(startElements).map( Element::getAccession ).filter( refNames::contains ).count();
            DynamicProperty inputSize = new DynamicProperty( FunctionalHubConstants.INPUT_GENES_DESCRIPTOR, Integer.class, inputCount );
            DynamicProperty totalSize = new DynamicProperty( FunctionalHubConstants.TOTAL_GENES_DESCRIPTOR, Integer.class, refNames.size() );
            map.forEach( (groupElement, set) -> {
                groupElement.setValue(new DynamicProperty(FunctionalHubConstants.GROUP_SIZE_DESCRIPTOR, Integer.class, set.size()));
                groupElement.setValue( inputSize );
                groupElement.setValue( totalSize );
            });
        }
        return result;
    }

    @Override
    public Properties[] getSupportedMatching(Properties input)
    {
        if(input.containsKey(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_RECORD))
        {
            Properties result = new Properties();
            result.setProperty(DataCollectionConfigConstants.URL_TEMPLATE, "de:"+parentPath+"/$id$");
            return new Properties[] {result};
        }
        return null;
    }
}

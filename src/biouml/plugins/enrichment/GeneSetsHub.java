package biouml.plugins.enrichment;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.util.HashMapWeakValues;

import com.developmentontheedge.beans.DynamicProperty;

public class GeneSetsHub extends BioHubSupport
{
    protected static final Logger log = Logger.getLogger(GeneSetsHub.class.getName());

    public static final String ENSEMBL_COLLECTION = "databases/Ensembl";

    private BioHub ensemblHub;
    private TargetOptions titleOptions;
    public static final String GENE_SET_COLLECTION_PROPERTY = "GeneSetCollection";
    private DataCollection<FunctionalCategory> categories = null;
    private boolean isInitialized = false;
    private boolean isWorking = false;
    Map<String, Element> elementCache = new HashMapWeakValues();

    public GeneSetsHub(Properties properties)
    {
        super(properties);
        categories = DataElementPath.create(properties.getProperty(GENE_SET_COLLECTION_PROPERTY)).optDataCollection(FunctionalCategory.class);
    }

    private void init()
    {
        if(isInitialized ) return;
        isInitialized = true;
        CollectionRecord ensemblTitleCollection = new CollectionRecord(ENSEMBL_COLLECTION, true);
        titleOptions = new TargetOptions(ensemblTitleCollection);
        ensemblHub = BioHubRegistry.getBioHub(titleOptions);
        isWorking = ensemblHub != null && categories != null && FunctionalCategory.class.isAssignableFrom(categories.getDataElementType());
    }

    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        DataElementPathSet dbList = dbOptions.getUsedCollectionPaths();
        if( dbList.size() > 0 )
        {
            for( DataElementPath cr : dbList )
            {
                if( !cr.toString().equals(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_RECORD) )
                {
                    return 0;
                }
            }
            init();
            if(!isWorking) return 0;
            return 5;
        }
        return 0;
    }

    @Override
    public Element[] getReference(Element startElement, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        init();
        if(!isWorking) return null;
        boolean ensemblOk = false;
        for( CollectionRecord cr : dbOptions.collections().filter( CollectionRecord::isUse ) )
        {
            titleOptions.setCollections(new CollectionRecord[]{cr});
            if( ensemblHub.getPriority(titleOptions) > 0 )
            {
                ensemblOk = true;
                CollectionRecord ensemblTitleCollection = new CollectionRecord(cr.getName(), true);
                titleOptions = new TargetOptions(ensemblTitleCollection);
                break;
            }
        }
        if(!ensemblOk) return null;
        Element[] re = ensemblHub.getReference(startElement, titleOptions, null, 1, -1);
        if(re == null) return null;
        Set<String> genes = StreamEx.of( re ).map( Element::getAccession ).toSet();
        return categories.stream()
            .filter( category -> category.containsAny( genes ) )
            .map( category -> elementCache.computeIfAbsent( category.getName(), n -> {
                Element element = new Element(category.getCompletePath());
                element.setValue("Description", category.getDescription());
                element.setValue(FunctionalHubConstants.GROUP_SIZE_PROPERTY, category.getSize());
                return element;
            })).toArray( Element[]::new );
    }

    @Override
    public Map<Element, Element[]> getReferences(Element[] startElements, TargetOptions dbOptions, String[] relationTypes, int maxLength,
            int direction)
    {
        boolean needHits = dbOptions.getUsedCollectionPaths().stream()
                .anyMatch( cr -> cr.toString().equals( FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_HITS_RECORD ) );
        elementCache.clear();
        Map<Element, Element[]> result = super.getReferences(startElements, dbOptions, relationTypes, maxLength, direction);
        if(!needHits) return result;
        Map<Element, Element[]> hgncRefs = ensemblHub.getReferences(startElements, titleOptions, null, 1, -1);
        Set<String> genes = StreamEx.ofValues( hgncRefs ).flatMap( Stream::of ).map( e -> e.getAccession().toUpperCase() ).toSet();
        FunctionalCategory joined = FunctionalCategory.createJoinedCategory(categories);
        try
        {
            DynamicProperty[] defaultProperties = {
                    new DynamicProperty(FunctionalHubConstants.INPUT_GENES_PROPERTY, Integer.class, joined.getHits(genes)),
                    new DynamicProperty(FunctionalHubConstants.TOTAL_GENES_PROPERTY, Integer.class, joined.getSize())
            };
            for(Object categoryObj: elementCache.values())
            {
                Element category = (Element)categoryObj;
                for(DynamicProperty property: defaultProperties)
                    category.setValue(property);
            }
        }
        catch(Exception e)
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return result;
    }
}

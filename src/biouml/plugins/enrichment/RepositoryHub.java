package biouml.plugins.enrichment;

import java.util.logging.Level;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 *
 */
public class RepositoryHub extends BioHubSupport
{
    private static final Logger log = Logger.getLogger(RepositoryHub.class.getName());
    private final DataElementPath parentPath;
    private final DataElementPath referenceCollection;
    public static final String REPOSITORY_HUB_NAME = "Repository folder";

    public RepositoryHub(Properties properties, DataElementPath collectionPath, DataElementPath referenceCollection)
    {
        super(properties);
        this.parentPath = collectionPath;
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
    
    protected DataElementPathSet getGroups(DataElementPath parentPath)
    {
        DataElementPathSet groups = new DataElementPathSet();
        
        for(DataElementPath child: parentPath.getChildren())
        {
            if(DataCollectionUtils.isAcceptable(child, null, TableDataCollection.class, EnsemblGeneTableType.class))
                groups.add(child);
            else if(DataCollectionUtils.isAcceptable(child, null, FolderCollection.class) || DataCollectionUtils.isAcceptable(child, null, VectorDataCollection.class))
                groups.addAll(getGroups(child));
        }
        return groups;
    }

    public DataElementPathSet getGroups()
    {
        return getGroups(parentPath);
    }
    
    private class RepositoryMatching
    {
        private Map<Element, Collection<String>> backMatching;
        private final Map<String, Element> accessions;
        private Set<String> totalGenes;
        private Set<String> matchedGenes;
        private final boolean hitsMode;
        
        public RepositoryMatching(Element[] startElements, boolean hitsMode)
        {
            this.hitsMode = hitsMode;
            accessions = new HashMap<>();
            for(Element element: startElements)
            {
                accessions.put(element.getAccession(), element);
            }
            if(this.hitsMode)
            {
                if(referenceCollection != null)
                {
                    List<String> referenceNames = referenceCollection.getDataCollection().getNameList();
                    totalGenes = new HashSet<>(referenceNames);
                    matchedGenes = new HashSet<>(accessions.keySet());
                    matchedGenes.retainAll(referenceNames);
                } else
                {
                    totalGenes = new HashSet<>();
                    matchedGenes = new HashSet<>();
                }
            }
        }

        public Map<Element, Element[]> getReferences()
        {
            DataElementPathSet groups = getGroups();
            
            backMatching = new HashMap<>();
            // metaGroup is a "folder"-group which contains all elements from the folder
            Map<ru.biosoft.access.core.DataElementPath, Collection<String>> metaGroups = new HashMap<>();
            for(DataElementPath group: groups)
            {
                try
                {
                    Collection<String> nameList = group.getDataCollection().getNameList();
                    for(DataElementPath metaGroupPath = group.getParentPath(); !metaGroupPath.equals(parentPath); metaGroupPath = metaGroupPath.getParentPath())
                    {
                        metaGroups.computeIfAbsent( metaGroupPath, k -> new HashSet<>() ).addAll( nameList );
                    }
                    addGroupMatching(group, nameList);
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, getClass().getSimpleName()+": Error fetching group "+group, e);
                }
            }
            EntryStream.of(metaGroups).forKeyValue(this::addGroupMatching);
            annotateGroups();
            return generateResult();
        }

        private void addGroupMatching(DataElementPath group, Collection<String> nameList)
        {
            Set<String> groupMembers = new HashSet<>(accessions.keySet());
            groupMembers.retainAll(nameList);
            Element groupElement = createGroupElement(group);
            if(hitsMode)
            {
                matchedGenes.addAll(groupMembers);
                totalGenes.addAll(nameList);
                groupElement.setValue(new DynamicProperty(FunctionalHubConstants.GROUP_SIZE_DESCRIPTOR, Integer.class, nameList.size()));
            }
            backMatching.put(groupElement, groupMembers);
        }

        private Map<Element, Element[]> generateResult()
        {
            return EntryStream.of( accessions ).invert()
                    .mapValues( element -> StreamEx.ofKeys( backMatching, val -> val.contains( element ) ).toArray( Element[]::new ) ).toMap();
        }

        private void annotateGroups()
        {
            if(hitsMode)
            {
                DynamicProperty inputSize = new DynamicProperty(FunctionalHubConstants.INPUT_GENES_DESCRIPTOR, Integer.class, matchedGenes.size());
                DynamicProperty totalSize = new DynamicProperty(FunctionalHubConstants.TOTAL_GENES_DESCRIPTOR, Integer.class, totalGenes.size());
                for(Element group: backMatching.keySet())
                {
                    group.setValue(inputSize);
                    group.setValue(totalSize);
                }
            }
        }
    }

    protected Element createGroupElement(final DataElementPath group)
    {
        Element element = new Element(group)
        {
            @Override
            public String getAccession()
            {
                return group.getPathDifference(parentPath);
            }
        };
        return element;
    }

    @Override
    public Map<Element, Element[]> getReferences(Element[] startElements, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        boolean hitsMode = StreamEx.of(dbOptions.getUsedCollectionPaths()).map(Object::toString).has(FunctionalHubConstants.FUNCTIONAL_CLASSIFICATION_HITS_RECORD);
        return new RepositoryMatching(startElements, hitsMode).getReferences();
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

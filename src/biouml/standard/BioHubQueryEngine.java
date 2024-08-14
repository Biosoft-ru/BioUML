package biouml.standard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import one.util.streamex.MoreCollectors;
import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.jobcontrol.JobControl;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.RelationType;
import ru.biosoft.access.biohub.TargetOptions;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.workbench.graphsearch.QueryEngineSupport;
import biouml.workbench.graphsearch.QueryOptions;
import biouml.workbench.graphsearch.SearchElement;

/**
 * QueryEngine based on BioHubs.
 * {@link BioHubRegistry} is used to find high priority {@link BioHub}
 */
public class BioHubQueryEngine extends QueryEngineSupport
{
    protected static final Logger log = Logger.getLogger(BioHubQueryEngine.class.getName());

    @Override
    public String getName( TargetOptions dbOptions )
    {
        BioHub hub = BioHubRegistry.getBioHub(dbOptions);
        if( hub != null )
        {
            return hub.getShortName();
        }
        return "BioHub search";
    }
    
    @Override
    public int canSearchLinked(TargetOptions dbOptions)
    {
        BioHub hub = BioHubRegistry.getBioHub(dbOptions);
        if( hub != null )
        {
            return hub.getPriority(dbOptions);
        }
        return 0;
    }

    @Override
    public SearchElement[] searchLinked(SearchElement[] startNodes, QueryOptions queryOptions, TargetOptions dbOptions,
            JobControl jobControl) throws Exception
    {
        BioHub bioHub = BioHubRegistry.getBioHub(dbOptions);
        if( bioHub != null )
        {
            List<SearchElement> elements = new ArrayList<>();
            if( startNodes != null )
            {
                if( jobControl != null )
                {
                    jobControl.setPreparedness(0);
                }
                double controlInterval = 100.0 / startNodes.length;
                int ind = 0;
                for( SearchElement startNode : startNodes )
                {

                    elements.add(startNode);
                    elements.addAll( internalSearchLinked( startNode, dbOptions, queryOptions.getDirection(), queryOptions.getDepth(),
                            bioHub ) );
                    ind++;
                    if( jobControl != null )
                    {
                        jobControl.setPreparedness((int) ( jobControl.getPreparedness() + ( ind * controlInterval ) ));
                    }
                }
                Set<ru.biosoft.access.core.DataElementPath> paths = StreamEx.of(startNodes).map( Element::getElementPath ).toSet();
                elements = StreamEx.of( elements ).remove( se -> paths.contains( se.getElementPath() ) )
                        .collect( MoreCollectors.distinctBy( se -> Arrays.asList( se.getPath(), se.getLinkedFromPath() ) ) );
                if( jobControl != null )
                {
                    jobControl.setPreparedness(100);
                }
            }
            return elements.toArray(new SearchElement[elements.size()]);
        }
        return null;
    }

    protected List<SearchElement> internalSearchLinked(SearchElement node, TargetOptions dbOptions, int direction, float depth,
            BioHub bioHub) throws Exception
    {
        Element[] hubResults = bioHub.getReference(node, dbOptions, getRelationTypes(node), (int) ( depth + 0.5f ), direction);
        if( hubResults == null )
            return Collections.emptyList();
        List<Element[]> minimalPaths = bioHub.getMinimalPaths( node, hubResults, dbOptions, getRelationTypes( node ), (int) ( depth + 0.5f ), direction );
        return StreamEx.of( minimalPaths ).nonNull().flatMap( Arrays::stream )
                .map( this::getSearchElement )
                .nonNull()
                .peek( se -> {
                    if(se.getLinkedFromPath() == null) {
                        se.setLinkedFromPath( node.getPath() );
                    }
                } )
                .toList();
    }

    private String[] getRelationTypes(SearchElement element)
    {
        if(element.getRelationType() != null)
            return new String[] {element.getRelationType()};
        return RelationType.allTypes();
    }

    @Override
    public int canSearchPath(TargetOptions dbOptions)
    {
        if( BioHubRegistry.getBioHub(dbOptions) != null )
        {
            return 20;
        }
        return 0;
    }

    @Override
    public SearchElement[] searchPath(SearchElement[] startNodes, QueryOptions queryOptions, TargetOptions dbOptions, JobControl jobControl)
            throws Exception
    {
        if( startNodes.length != 2 )
        {
            log.log(Level.SEVERE, "startNodes array length must be 2");
            return null;
        }

        BioHub bioHub = BioHubRegistry.getBioHub(dbOptions);
        if( bioHub != null )
        {
            return internalSearchPath(startNodes[0], startNodes[1], dbOptions, queryOptions.getDirection(), queryOptions.getDepth(),
                    new ArrayList<SearchElement>(), bioHub, jobControl);
        }
        return null;
    }

    protected SearchElement[] internalSearchPath(SearchElement start, SearchElement end, TargetOptions dbOptions, int direction,
            float depth, List<SearchElement> exceptList, BioHub bioHub, JobControl jobControl) throws Exception
    {
        if( jobControl != null )
        {
            jobControl.setPreparedness(0);
        }
        //first iteration: direct records from BioHub
        Element[] path = bioHub.getMinimalPath(start, end, dbOptions, getRelationTypes(start), (int) ( depth + 0.5f ), direction);

        if( path != null )
        {
            SearchElement[] result = StreamEx.of(path).map(this::getSearchElement).toArray( SearchElement[]::new );
            if( jobControl != null )
            {
                jobControl.setPreparedness(100);
            }
            return result;
        }

        //second iteration: combine from 2 path
        //TODO: implement this correctly
        
        
        return null;
    }

    protected SearchElement getSearchElement(Element e)
    {
        Base base = e.getElementPath().optDataElement(Base.class);
        if( base == null )
            base = new Reaction(null, e.getAccession());
        SearchElement se = new SearchElement(base);
        se.setLinkedDirection(e.getLinkedDirection());
        se.setLinkedFromPath(e.getLinkedFromPath());
        se.setLinkedLength(e.getLinkedLength());
        se.setLinkedPath(e.getLinkedPath());
        se.setRelationType(e.getRelationType());
        return se;
    }
}

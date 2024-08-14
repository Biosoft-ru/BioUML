package biouml.plugins.keynodes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.standard.type.Species;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.BioHubRegistry.BioHubInfo;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@ClassIcon("resources/FindShortestPathsSets.gif")
public class ShortestPathsBetweenSetsFinder extends AnalysisMethodSupport<ShortestPathsBetweenSetsFinderParameters> implements PathGenerator
{
    public ShortestPathsBetweenSetsFinder(DataCollection<?> origin, String name)
    {
        super( origin, name, new ShortestPathsBetweenSetsFinderParameters() );
    }

    @Override
    public void validateParameters()
    {
        super.validateParameters();
        checkNotEmptyCollection( "sourcePath" );
        checkNotEmptyCollection( "endSet" );
        checkNotEmpty( "bioHub" );
        checkRange( "maxRadius", 1, 20 );

        BioHubInfo bhi = parameters.getBioHub();
        if( bhi == null )
        {
            throw new IllegalArgumentException( "No biohub selected" );
        }
        KeyNodesHub<?> bioHub = parameters.getKeyNodesHub();
        if( bioHub == null )
        {
            throw new IllegalArgumentException( "Select custom biohub collection" );
        }

        TableDataCollection startSet = parameters.getSource();
        TableDataCollection endSet = parameters.getTarget();
        if( startSet.getSize() == 0 )
            throw new IllegalArgumentException( "Starts set is empty or was loaded with errors" );
        if( endSet.getSize() == 0 )
            throw new IllegalArgumentException( "Ends set is empty or was loaded with errors" );

        ReferenceType startType = ReferenceTypeRegistry.optReferenceType( startSet.getReferenceType() );
        ReferenceType endType = ReferenceTypeRegistry.optReferenceType( endSet.getReferenceType() );
        if( isSetTypeUnsupported( startType, bioHub ) || isSetTypeUnsupported( endType, bioHub ) )
        {
            ReferenceType[] supportedTypes = bioHub.getSupportedInputTypes();
            String supportedStr = Stream.of( supportedTypes ).map( ReferenceType::getDisplayName ).map( n -> '\t' + n + '\n' )
                    .collect( Collectors.joining() );
            throw new IllegalArgumentException(
                    "Search collection " + bioHub.getName() + " does not support objects of given type. \nAcceptable "
                            + ( supportedTypes.length > 1 ? "types are\n" : "type is\n" ) + supportedStr + "Try to convert table first." );
        }
    }
    private boolean isSetTypeUnsupported(ReferenceType type, KeyNodesHub<?> bioHub)
    {
        if( type == null )
            return false;
        ReferenceType[] types = bioHub.getSupportedMatching( type );
        return types == null || types.length == 0;
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        List<String> startNames = parameters.getSource().getNameList();
        List<String> endNames = parameters.getTarget().getNameList();
        log.info( "Searching for paths..." );
        jobControl.pushProgress( 0, 95 );
        TableDataCollection result = createTable( startNames, endNames );
        if( jobControl.isStopped() )
            return null;
        if( result == null )
        {
            log.info( "Result was not created" );
            return null;
        }

        this.writeProperties( result );
        AnalysisParametersFactory.writePersistent( result, this );
        result.getInfo().setNodeImageLocation( getClass(), "resources/keynodes.gif" );
        result.getInfo().getProperties().setProperty( DataCollectionUtils.SPECIES_PROPERTY, parameters.getSpecies().getLatinName() );

        CollectionFactoryUtils.save( result );
        jobControl.popProgress();
        log.info( "DataCollection " + result.getName() + " created" );
        return result;
    }

    private TableDataCollection createTable(List<String> startNames, List<String> endNames) throws Exception
    {
        TargetOptions dbOptions = KeyNodeAnalysis.getDBOptions();
        KeyNodesHub<?> bioHub = parameters.getKeyNodesHub();
        int maxRadius = parameters.getMaxRadius();
        int direction = parameters.getSearchDirection();

        final TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getOutputPath() );
        result.getColumnModel().addColumn( "Hits", StringSet.class ).setHidden( true );
        result.getColumnModel().addColumn( "Hit names", StringSet.class );

        List<Element> startElements = StreamEx.of( startNames ).map( n -> new Element( "stub/%//" + n ) ).toList();
        final Element[] endElements = StreamEx.of( endNames ).map( n -> new Element( "stub/%//" + n ) ).toArray( Element[]::new );
        String[] relTypes = new String[] {Species.getDefaultSpecies( parameters.getSourcePath().optDataCollection() ).getLatinName()};

        jobControl.setPreparedness( 3 );
        jobControl.forCollection( startElements, start -> {
            Set<Element> endSet = new HashSet<>();
            try
            {
                List<Element[]> paths = bioHub.getMinimalPaths( start, endElements, dbOptions, relTypes, maxRadius, direction );
                for( Element[] path : paths )
                {
                    if( path == null || path.length == 0 || path[0].getAccession().equals( path[path.length - 1].getAccession() ) )
                        continue;
                    Element endElement = start.getAccession().equals( path[0].getAccession() ) ? path[path.length - 1] : path[0];
                    endSet.add( endElement );
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE,  "Error while path creating for " + start.getAccession(), e );
            }
            StringSet hits = endSet.stream().map( Element::getAccession ).sorted( String::compareTo )
                    .collect( Collectors.toCollection( StringSet::new ) );
            StringSet hitNames = endSet.stream().map( bioHub::getElementTitle ).sorted( String::compareTo )
                    .collect( Collectors.toCollection( StringSet::new ) );

            TableDataCollectionUtils.addRow( result, start.getAccession(), new Object[] {hits, hitNames}, true );
            return true;
        } );
        result.finalizeAddition();

        if( jobControl.isStopped() )
            return null;

        jobControl.setPreparedness( 95 );
        return result;
    }

    @Override
    public List<Element[]> generatePaths(String startElement, StringSet hits)
    {
        Element element = new Element( "stub/%//" + startElement );
        Element[] hitsElements = hits.stream().map( hit -> new Element( "stub/%//" + hit ) ).toArray( Element[]::new );
        return parameters.getKeyNodesHub().getMinimalPaths( element, hitsElements, KeyNodeAnalysis.getDBOptions(),
                new String[] {Species.getDefaultSpecies( parameters.getSourcePath().optDataCollection() ).getLatinName()},
                parameters.getMaxRadius(),
                parameters.getSearchDirection() );
    }

    @Override
    public StringSet getKeysFromName(String name)
    {
        return StreamEx.of( name ).toCollection( StringSet::new );
    }

    @Override
    public List<Element> getAllReactions(String startElement, StringSet hits)
    {
        Element element = new Element( "stub/%//" + startElement );
        Element[] hitsElements = hits.stream().map( hit -> new Element( "stub/%//" + hit ) ).toArray( Element[]::new );
        return parameters.getKeyNodesHub().getAllReactions( element, hitsElements, KeyNodeAnalysis.getDBOptions(),
                new String[] {Species.getDefaultSpecies( parameters.getSourcePath().optDataCollection() ).getLatinName()},
                parameters.getMaxRadius(),
                parameters.getSearchDirection() );
    }

    @Override
    public KeyNodesHub<?> getKeyNodesHub()
    {
        return parameters.getKeyNodesHub();
    }
}

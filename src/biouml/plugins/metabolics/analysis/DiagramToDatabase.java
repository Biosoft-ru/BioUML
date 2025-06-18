package biouml.plugins.metabolics.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.ProtectedModule;
import biouml.plugins.lucene.LuceneUtils;
import biouml.plugins.metabolics.MetabolicModuleType;
import biouml.standard.type.Complex;
import biouml.standard.type.Concept;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.Referrer;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Species;
import biouml.standard.type.Substance;
import biouml.standard.type.access.ReactionMatchingHub;
import biouml.standard.type.access.StandardMatchingHub;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.Repository;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.BioHubRegistry.MatchingStep;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.analysis.type.ProteinTableType;
import ru.biosoft.analysis.type.ReactionType;
import ru.biosoft.analysis.type.SubstanceType;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;

public class DiagramToDatabase extends AnalysisMethodSupport<DiagramToDatabase.DiagramToDatabaseParameters>
{
    private static final String ENZYME_MIR_ID = "MIR:00000004";
    private static final String UNIPROT_MIR_ID = "MIR:00000005";
    private static final String CHEBI_MIR_ID = "MIR:00000002";
    private static final DiagramIdExtractor[] EXTRACTORS = {
        new DefaultIdExtractor(),
        new HMRIdExtractor(),
        new ReconIdExtractor()
    };

    public interface DiagramIdExtractor
    {
        String extractRawId(Node element);

        @Nonnull
        String getDefaultIdType();

        String getElementName(String rawId);

        List<String> parseRawId(String rawId);

        String getReferenceType(Logger log, String id);
    }

    public static class DefaultIdExtractor implements DiagramIdExtractor
    {
        @Override
        public String extractRawId(Node element)
        {
            return element.getName();
        }

        @Override
        public String getElementName(String rawId)
        {
            return rawId;
        }

        @Override
        public List<String> parseRawId(String rawId)
        {
            return Collections.singletonList( rawId );
        }

        @Override
        public String getReferenceType(Logger log, String id)
        {
            return null;
        }

        @Override
        public @Nonnull String getDefaultIdType()
        {
            return "Genes: Entrez";
        }

        @Override
        public String toString()
        {
            return "Default";
        }
    }
    public static class HMRIdExtractor implements DiagramIdExtractor
    {
        @Override
        public String extractRawId(Node element)
        {
            return element.getTitle();
        }

        @Override
        public String getElementName(String rawId)
        {
            return rawId;
        }

        @Override
        public List<String> parseRawId(String rawId)
        {
            return Collections.singletonList( rawId );
        }

        @Override
        public String getReferenceType(Logger log, String id)
        {
            return id.startsWith( "ENSG" ) ? "Genes: Ensembl" : null;
        }

        @Override
        public @Nonnull String getDefaultIdType()
        {
            return "Genes: Ensembl";
        }

        @Override
        public String toString()
        {
            return "HMR";
        }
    }

    public static class ReconIdExtractor implements DiagramIdExtractor
    {
        @Override
        public String extractRawId(Node element)
        {
            return element.getName();
        }

        @Override
        public String getElementName(String rawId)
        {
            if(rawId.startsWith( "M_" ))
                return rawId.substring( 0, rawId.length()-2 );
            String[] parts = parse( rawId );
            if(parts.length == 1)
                return rawId;
            return IntStreamEx.ofIndices( parts ).filter( i -> i % 3 == 1 ).elements( parts ).joining( "_" );
        }

        private String[] parse(String rawId)
        {
            return StreamEx.split(rawId, '_')
                    .collapse( (a, b) -> a.equals( "NM" ), (a, b) -> a+"_"+b ).toArray( String[]::new );
        }

        @Override
        public List<String> parseRawId(String rawId)
        {
            if(rawId.startsWith( "M_") )
                return Collections.singletonList( rawId.substring( 0, rawId.length()-2 ) );
            if(rawId.length() == 1)
                return Collections.singletonList( rawId );
            String[] parts = parse( rawId );
            return IntStreamEx.ofIndices( parts ).filter( i -> i % 3 == 1 ).elements( parts )
                    .map( s -> s.replaceFirst( "^HG(\\d+)HT\\d+$", "$1" ) ).toList();
        }

        @Override
        public String getReferenceType(Logger log, String id)
        {
            if(id.startsWith( "M_" ))
                return null;
            if(id.matches( "[a-z]" ))
                return null; // compartment
            if(id.matches( "\\d+" ))
                return "Genes: Entrez";
            if( id.matches("[ABD-NR-Z]\\d{5}") || id.matches("[A-H][A-Z]\\d{6}") )
                return "Genes: GenBank";
            if( id.matches("(NM|NR|XM|XR)\\_\\d{6}(\\.\\d+|)")
                    || id.matches("(NM|XM)\\_\\d{9}(\\.\\d+|)"))
                return "Transcripts: RefSeq";
            ReferenceType detected = ReferenceTypeRegistry.detectReferenceType( id );
            if(detected == ReferenceTypeRegistry.getDefaultReferenceType())
            {
                log.warning( "Unable to detect type of "+id );
            } else
            {
                log.info( "Type of "+id+" autodetected: "+detected );
            }
            return detected.toString();
        }

        @Override
        public @Nonnull String getDefaultIdType()
        {
            return "Genes: Entrez";
        }

        @Override
        public String toString()
        {
            return "Recon";
        }

    }

    public DiagramToDatabase(DataCollection<?> origin, String name)
    {
        super( origin, name, new DiagramToDatabaseParameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        checkNotEmpty( "databaseName" );
        if( CollectionFactoryUtils.getDatabases().contains( getParameters().getDatabaseName() ) )
        {
            throw new IllegalArgumentException( "Database " + getParameters().getDatabaseName() + " already exists" );
        }
    }

    private static <T extends Referrer> T create(Module module, DiagramElement de, Class<T> clazz, String title)
    {
        T base;
        try
        {
            base = clazz.getConstructor( ru.biosoft.access.core.DataCollection.class, String.class ).newInstance( module.getCategory( clazz ),
                    title );
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
        base.setTitle( de.getTitle() );
        base.setComment( de.getComment() );
        if( de.getKernel() instanceof Referrer )
        {
            Referrer ref = (Referrer)de.getKernel();
            DatabaseReference[] dr = ref.getDatabaseReferences();
            if( dr != null )
            {
                if( base instanceof Concept )
                    ( (Concept)base ).setSynonyms( StreamEx.of( dr ).flatMap( r -> Stream.of( r.getAc(), r.getId() ) ).distinct()
                            .joining( ", " ) );
                base.setDatabaseReferences( dr.clone() );
            }
        }
        return base;
    }

    Map<List<String>, MatchingStep[]> matchings = new HashMap<>();

    private String[] match(String id, String sourceType, String targetType)
    {
        MatchingStep[] steps = matchings.computeIfAbsent( Arrays.asList(sourceType, targetType), key -> {
            Properties input = BioHubSupport.createProperties( getParameters().getSpecies().getLatinName(), sourceType );
            Properties output = BioHubSupport.createProperties( getParameters().getSpecies().getLatinName(), targetType );
            MatchingStep[] matchingPath = BioHubRegistry.getMatchingPath( input, output );
            if(matchingPath == null) {
                log.warning( "Requested matching path not found: "+sourceType+" -> "+targetType );
                return new MatchingStep[1];
            }
            log.info( "Cached matching path: "+sourceType+" -> "+targetType );
            return matchingPath;
        });
        if(steps != null) {
            if(steps.length == 1 && steps[0] == null)
                return null;
            Map<String, String[]> refs = BioHubRegistry.getReferences( new String[] {id}, steps, null );
            return refs == null ? null : refs.get( id );
        }
        return null;
    }

    private void addArrayElement(DynamicPropertySet dps, String propertyName, String element)
    {
        String[] oldValue = (String[])dps.getValue( propertyName );
        if(oldValue == null)
        {
            dps.add( new DynamicProperty( propertyName, String[].class, new String[] {element} ) );
        } else if(!Arrays.asList(oldValue).contains( element ))
        {
            dps.setValue( propertyName, StreamEx.of(oldValue).append( element ).toArray( String[]::new ) );
        }
    }

    private void addSynonyms(Concept c, Stream<String> synonyms)
    {
        c.setSynonyms( StreamEx.split( TextUtil2.nullToEmpty( c.getSynonyms() ), ", " ).append( synonyms ).distinct().joining( ", " ) );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        Diagram d = getParameters().getDiagramPath().getDataElement( Diagram.class );
        DiagramIdExtractor extractor = getParameters().getExtractor();
        String defaultReferenceType = extractor.getDefaultIdType();
        String defaultDbId = ReferenceTypeRegistry.getReferenceType( defaultReferenceType ).getMiriamId();

        MetabolicModuleType moduleType = new MetabolicModuleType();
        String dbName = getParameters().getDatabaseName();
        Module module = moduleType.createModule( (Repository)CollectionFactoryUtils.getDatabases(), dbName );
        module.getInfo().getProperties().setProperty( QuerySystem.QUERY_SYSTEM_CLASS, "biouml.plugins.lucene.LuceneQuerySystemImpl" );
        module.getInfo().getProperties().setProperty( "data-collection-listener", "biouml.plugins.lucene.LuceneInitListener" );
        module.getInfo().getProperties().setProperty( "lucene-directory", "luceneIndex" );
        module.getInfo().getProperties().setProperty( "graph-search", "true" );

        module.getInfo().getProperties().setProperty( "bioHub.search",
                "biouml.plugins.keynodes.graph.CollectionBioHub;name="+dbName+" hub;diagramType="+MetabolicModuleType.NOTATION_NAME );
        module.getInfo().getProperties().setProperty( "bioHub.matching",
                StandardMatchingHub.class.getName()+";name="+dbName+" protein matching;collection=Data/protein;target="+defaultReferenceType );
        module.getInfo().getProperties().setProperty( "bioHub.matchingSubst",
                StandardMatchingHub.class.getName()+";name="+dbName+" protein matching;collection=Data/substance;target=Substances: ChEBI" );
        module.getInfo().getProperties().setProperty( "bioHub.matchingReact",
                ReactionMatchingHub.class.getName()+";name="+dbName+" reaction matching;collection=Data/reaction;target=Proteins: "+ dbName );

        module.getInfo().getProperties().setProperty( DataCollectionConfigConstants.PLUGINS_PROPERTY, "biouml.plugins.metabolics" );

        module.getInfo().getProperties().setProperty( "referenceType.protein", "Proteins;icon="+IconFactory.getClassIconId( ProteinTableType.class ) );
        module.getInfo().getProperties().setProperty( "referenceType.substance", "Substances;icon="+IconFactory.getClassIconId( SubstanceType.class ) );
        module.getInfo().getProperties().setProperty( "referenceType.reaction", "Reactions;icon="+IconFactory.getClassIconId( ReactionType.class ) );

        module.getCategory( Protein.class ).getInfo().getProperties().setProperty( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, "Proteins: "+dbName );
        module.getCategory( Substance.class ).getInfo().getProperties().setProperty( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, "Substances: "+dbName );
        module.getCategory( Reaction.class ).getInfo().getProperties().setProperty( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY, "Reactions: " + dbName );

        CollectionFactoryUtils.save( module );
        Map<String, Referrer> referrers = new HashMap<>();

        String uniprotType = "Proteins: UniProt";
        String expasyType = "Enzymes: ExPASy";
        String geneSymbolType = "Genes: Gene symbol";
        String chebiType = "Substances: ChEBI";
        log.info( "Processing nodes..." );
        jobControl.pushProgress( 0, 75 );
        List<Node> nodes = d.recursiveStream().without( d ).select( Node.class )
                .remove( de -> de.getCompartment().getKernel().getType().equals( "complex" ) )
                .remove( de -> de.getKernel().getType().equals( "reaction" ) ).toList();
        jobControl.forCollection( nodes, de -> {
            String kernelType = de.getKernel().getType();
            Referrer element = null;
            String rawId = extractor.extractRawId( de );
            String name = extractor.getElementName( rawId );
            List<String> ids = extractor.parseRawId( rawId );
            switch( kernelType )
            {
                case "compartment":
                    element = create( module, de, biouml.standard.type.Compartment.class, name );
                    break;
                case "molecule-substance":
                case "simple chemical":
                    element = create( module, de, Substance.class, name );
                    break;
                case "macromolecule":
                    element = create( module, de, Protein.class, name );
                    break;
                case "complex":
                    element = create( module, de, Complex.class, name );
                    break;
                case "source-sink":
                    break;
                default:
                    log.warning( "Unknown element: " + de.getCompleteNameInDiagram()+" (type: "+kernelType+")" );
            }
            if( element != null )
            {
                if(element instanceof Substance && element.getDatabaseReferences() != null
                        && StreamEx.of(element.getDatabaseReferences()).noneMatch( dr -> dr.getDatabaseName().equals(CHEBI_MIR_ID) ))
                {
                    DatabaseReference[] newRefs = StreamEx.of(element.getDatabaseReferences())
                        .mapToEntry(dr -> ReferenceTypeRegistry.byMiriamId( dr.getDatabaseName() ), dr -> dr.getId())
                        .flatMapKeys(StreamEx::of)
                        .mapKeyValue((type, id) -> match( id, type.toString(), chebiType ))
                        .nonNull()
                        .flatMap(Arrays::stream)
                        .map(id -> new DatabaseReference( CHEBI_MIR_ID, id ))
                        .prepend( element.getDatabaseReferences() )
                        .toArray(DatabaseReference[]::new);
                    element.setDatabaseReferences( newRefs );
                }
                if(ids.size() == 1)
                { // non-complex
                    String id = ids.get( 0 );
                    String type = extractor.getReferenceType( log, id );
                    if( type != null )
                    {
                        String[] result = match(id, type, geneSymbolType );
                        if(result == null || result.length == 0)
                        {
                            log.warning( "Not matched: "+id+"/"+type );
                        } else
                        {
                            element.setTitle( result[0] );
                            addSynonyms( (Concept)element, StreamEx.of(result).append( id ).without( result[0] ) );
                        }
                        result = match(id, type, uniprotType );
                        if(result != null && result.length > 0)
                        {
                            element.addDatabaseReferences( StreamEx.of( result )
                                    .map( ref -> new DatabaseReference( UNIPROT_MIR_ID, ref ) ).toArray( DatabaseReference[]::new ) );
                            addSynonyms( (Concept)element, Stream.of(result) );
                        }
                        result = match(id, type, defaultReferenceType );
                        if(result != null && result.length > 0)
                        {
                            if(!type.equals( defaultReferenceType ))
                            {
                                log.info( "Matching salvage for "+de.getName()+": "+id+" => "+type );
                            }
                            element.addDatabaseReferences( StreamEx.of( result )
                                    .map( ref -> new DatabaseReference( defaultDbId, ref ) ).toArray( DatabaseReference[]::new ) );
                        }
                        result = match(id, type, expasyType );
                        if(result != null && result.length > 0)
                        {
                            element.getAttributes().add( new DynamicProperty( "EC", String.class, result[0] ) );
                            element.addDatabaseReferences( StreamEx.of( result )
                                    .map( ref -> new DatabaseReference( ENZYME_MIR_ID, ref ) ).toArray( DatabaseReference[]::new ) );
                            addSynonyms( (Concept)element, Stream.of(result) );
                        } else
                        {
                            element.getAttributes().add( new DynamicProperty( "EC", String.class, "" ) );
                        }
                    }
                } else
                {
                    // complex - TODO
                    if(!kernelType.equals( "complex" ))
                    {
                        log.warning( "Unexpected: "+de.getCompleteNameInDiagram()+" parsed as complex, but it's type is "+kernelType );
                    }
                    List<String> titleParts = new ArrayList<>();
                    for(String id : ids)
                    {
                        String type = extractor.getReferenceType( log, id );
                        if( type != null )
                        {
                            String[] result = match(id, type, geneSymbolType );
                            if(result != null && result.length > 0)
                            {
                                titleParts.add( result[0] );
                            } else
                            {
                                titleParts.add( id );
                            }
                        }
                    }
                    element.setTitle( String.join( ":", titleParts ) );
                }
                Referrer existing = element.getCompletePath().optDataElement( Referrer.class );
                if(existing != null)
                {
                    addArrayElement( existing.getAttributes(), "compartments", de.getCompartment().getTitle() );
                    existing.addDatabaseReferences( element.getDatabaseReferences() );
                    referrers.put( de.getCompleteNameInDiagram(), existing );
                } else
                {
                    addArrayElement( element.getAttributes(), "compartments", de.getCompartment().getTitle() );
                    referrers.put( de.getCompleteNameInDiagram(), element );
                    CollectionFactoryUtils.save( element );
                }
            }
            return true;
        });
        jobControl.popProgress();
        log.info( "Mark stop-molecules" );
        List<Node> reactions = d.recursiveStream().select( Node.class ).filter( n -> n.getKernel().getType().equals( "reaction" ) ).toList();

        List<Substance> stopList;
        if(parameters.isUseThreshold())
        {
            stopList = StreamEx.of(reactions).map( Node::getKernel ).select( Reaction.class )
                    .flatMap( r -> Arrays.stream( r.getSpecieReferences() ) )
                    .map( SpecieReference::getSpecie )
                    .map( referrers::get )
                    .select( Substance.class )
                    .distinct( parameters.getStopMoleculeThreshold() )
                    .nonNull().toList();
        } else
        {
            stopList = parameters.getStopPath().getDataCollection().names()
                    .map( module.getCategory( Substance.class ).getCompletePath()::getChildPath )
                    .map( DataElementPath::getDataElement )
                    .filter( Substance.class::isInstance ).map( de -> (Substance)de ).collect( Collectors.toList() );
        }

//        Map<Substance, Long> stopCounts = StreamEx.of(reactions).map( Node::getKernel ).select( Reaction.class )
//                .flatMap( r -> Arrays.stream( r.getSpecieReferences() ) )
//                .map( SpecieReference::getSpecie )
//                .map( referrers::get )
//                .select( Substance.class )
//                .nonNull()
//                .groupingBy( Function.identity(), Collectors.counting() );
//
//        EntryStream.of( stopCounts )
//            .filterValues( count -> count >= parameters.getStopMoleculeThreshold() )
//            .sorted( Map.Entry.comparingByValue( Comparator.reverseOrder() ) )
//            .mapKeyValue( (substance, count) -> substance.getName()+" ("+substance.getTitle()+"): "+count )
//            .forEach( log::info );

        log.info( "Stop-molecules count: " + stopList.size() );
        for( Substance ref : stopList )
        {
            ref.getAttributes().add( new DynamicProperty( "StopMolecule", Boolean.class, Boolean.TRUE ) );
            CollectionFactoryUtils.save( ref );
        }

        log.info( "Processing reactions..." );
        jobControl.pushProgress( 80, 85 );
        jobControl.forCollection( reactions, de -> {
            Reaction r = create( module, de, Reaction.class, de.getName() );
            Reaction src = de.getKernel().cast( Reaction.class );
            r.setReversible( src.isReversible() );
            boolean stopReaction = StreamEx.of(src.getSpecieReferences())
                .map( SpecieReference::getRole )
                .distinct( parameters.getStopReactionThreshold() )
                .anyMatch( Predicate.isEqual( SpecieReference.REACTANT ).or( Predicate.isEqual( SpecieReference.PRODUCT ) ) );
            if(stopReaction)
            {
                log.info( "Reaction will be ignored in search: "+r.getName() );
                r.getAttributes().add( new DynamicProperty( "StopReaction", Boolean.class, Boolean.TRUE ) );
            }
            for( SpecieReference sr : src.getSpecieReferences() )
            {
                SpecieReference newSr = new SpecieReference( r, sr.getName() );
                Referrer ref = referrers.get( sr.getSpecie() );
                if( ref == null )
                {
                    log.warning( "Unable to find specie " + sr.getSpecie() + " for reaction " + de.getCompleteNameInDiagram() );
                    continue;
                }
                newSr.setSpecie( ref.getCompletePath().getPathDifference( module.getCompletePath() ) );
                newSr.setRole( sr.getRole() );
                newSr.setStoichiometry( sr.getStoichiometry() );
                r.put( newSr );
            }
            CollectionFactoryUtils.save( r );
            return true;
        });

        log.info( "Cleanup" );
        jobControl.setPreparedness( 85 );
        for( ru.biosoft.access.core.DataElement de : ( (DataCollection<?>)module.get( Module.DATA ) ).stream()
                .collect( Collectors.toList() ) )
        {
            if( de instanceof DataCollection )
            {
                DataCollection<?> dc = (DataCollection<?>)de;
                if( dc.isEmpty() )
                    dc.getCompletePath().remove();
                else
                {
                    dc.getInfo().getProperties().setProperty( "lucene-indexes", "name;title;completeName;synonyms;description;comment" );
                    CollectionFactoryUtils.save( de );
                }
            }
        }
        log.info( "Build lucene indexes" );
        jobControl.setPreparedness( 90 );
        jobControl.pushProgress( 90, 100 );
        LuceneUtils.buildIndexes(module, jobControl, log);
        jobControl.popProgress();

        ProtectedModule.protect( module, 1 );

        jobControl.setPreparedness( 100 );

        return null; // do not return module as we don't want to store the analysis metadata in the result
    }

    @SuppressWarnings ( "serial" )
    public static class DiagramToDatabaseParameters extends AbstractAnalysisParameters
    {
        private DataElementPath diagramPath;
        private String databaseName = "";
        private DiagramIdExtractor extractor = EXTRACTORS[0];
        private Species species = Species.getDefaultSpecies( null );
        private boolean useThreshold = false;
        private DataElementPath stopPath;
        private int stopMoleculeThreshold = 50;
        private int stopReactionThreshold = 20;

        @PropertyName ( "Diagram" )
        @PropertyDescription ( "Path to the diagram you want to convert to the database" )
        public DataElementPath getDiagramPath()
        {
            return diagramPath;
        }
        public void setDiagramPath(DataElementPath diagramPath)
        {
            Object oldValue = this.diagramPath;
            this.diagramPath = diagramPath;
            firePropertyChange( "diagramPath", oldValue, this.diagramPath );
        }

        @PropertyName ( "Database name" )
        @PropertyDescription ( "The name of the newly-created database" )
        public String getDatabaseName()
        {
            return databaseName;
        }
        public void setDatabaseName(String databaseName)
        {
            Object oldValue = this.databaseName;
            this.databaseName = databaseName;
            firePropertyChange( "databaseName", oldValue, this.databaseName );
        }

        @PropertyName ( "Species" )
        public Species getSpecies()
        {
            return species;
        }

        public void setSpecies(Species species)
        {
            Object oldValue = this.species;
            this.species = species;
            firePropertyChange( "species", oldValue, this.species );
        }

        @PropertyName("ID extractor")
        @PropertyDescription("An algorithm which matches diagram IDs (diagram-specific)")
        public DiagramIdExtractor getExtractor()
        {
            return extractor;
        }
        public void setExtractor(DiagramIdExtractor extractor)
        {
            Object oldValue = this.extractor;
            this.extractor = extractor;
            firePropertyChange( "extractor", oldValue, this.extractor );
        }

        @PropertyName("Stop-reaction threshold")
        @PropertyDescription("If reaction has more reactants or products than the specified number, it will be marked as ignored")
        public int getStopReactionThreshold()
        {
            return stopReactionThreshold;
        }
        public void setStopReactionThreshold(int stopReactionThreshold)
        {
            Object oldValue = this.stopReactionThreshold;
            this.stopReactionThreshold = stopReactionThreshold;
            firePropertyChange( "stopReactionThreshold", oldValue, this.stopReactionThreshold );
        }

        @PropertyName("Stop-molecule threshold")
        @PropertyDescription("If molecule participates in more reactions than specified it will be marked as stop-molecule")
        public int getStopMoleculeThreshold()
        {
            return stopMoleculeThreshold;
        }
        public void setStopMoleculeThreshold(int stopMoleculeThreshold)
        {
            Object oldValue = this.stopMoleculeThreshold;
            this.stopMoleculeThreshold = stopMoleculeThreshold;
            firePropertyChange( "stopMoleculeThreshold", oldValue, this.stopMoleculeThreshold );
        }

        @PropertyName("Use threshold for stop-molecules")
        @PropertyDescription("Uncheck to specify stop-molecules list explicitly")
        public boolean isUseThreshold()
        {
            return useThreshold;
        }
        public void setUseThreshold(boolean useThreshold)
        {
            Object oldValue = this.useThreshold;
            this.useThreshold = useThreshold;
            firePropertyChange( "useThreshold", oldValue, this.useThreshold );
            firePropertyChange( "*", null, null );
        }

        @PropertyName("Stop molecules list")
        @PropertyDescription("Path to the table containing stop-molecules IDs")
        public DataElementPath getStopPath()
        {
            return stopPath;
        }
        public void setStopPath(DataElementPath stopPath)
        {
            Object oldValue = this.stopPath;
            this.stopPath = stopPath;
            firePropertyChange( "stopPath", oldValue, this.stopPath );
        }

        public boolean isThresholdHidden()
        {
            return !useThreshold;
        }
    }

    public static class DiagramIdSelector extends GenericComboBoxEditor
    {
        @Override
        protected Object[] getAvailableValues()
        {
            return EXTRACTORS.clone();
        }
    }

    public static class DiagramToDatabaseParametersBeanInfo extends BeanInfoEx2<DiagramToDatabaseParameters>
    {
        public DiagramToDatabaseParametersBeanInfo()
        {
            super( DiagramToDatabaseParameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "diagramPath" ).inputElement( Diagram.class ).add();
            property( "extractor" ).editor( DiagramIdSelector.class ).simple().add();
            property( DataElementComboBoxSelector.registerSelector( "species", beanClass, Species.SPECIES_PATH ) ).add();
            property( "useThreshold" ).add();
            property( "stopMoleculeThreshold" ).hidden( "isThresholdHidden" ).add();
            property( "stopPath" ).inputElement( TableDataCollection.class ).hidden( "isUseThreshold" ).add();
            property( "stopReactionThreshold" ).add();
            property( "databaseName" ).auto( "$diagramPath/name$" ).add();
        }
    }
}

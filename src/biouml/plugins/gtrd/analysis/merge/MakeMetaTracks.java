package biouml.plugins.gtrd.analysis.merge;

import java.awt.Color;
import java.beans.PropertyDescriptor;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.ensembl.tabletype.UniprotProteinTableType;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.ExperimentType;
import biouml.plugins.gtrd.HistonesExperiment;
import biouml.plugins.gtrd.analysis.merge.MergePeakCallers.Caller;
import biouml.plugins.gtrd.analysis.merge.MergePeakCallers.CallerClusters;
import biouml.standard.type.Species;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.MergedTrack;
import ru.biosoft.bsa.Position;
import ru.biosoft.bsa.ResizedTrack;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.UnionTrack;
import ru.biosoft.bsa.analysis.SiteModelsToProteins;
import ru.biosoft.bsa.analysis.SiteModelsToProteinsParameters;
import ru.biosoft.bsa.analysis.SiteSearchAnalysis;
import ru.biosoft.bsa.analysis.SiteSearchAnalysisParameters;
import ru.biosoft.bsa.analysis.SortSqlTrack;
import ru.biosoft.bsa.project.ProjectAsLists;
import ru.biosoft.bsa.project.Region;
import ru.biosoft.bsa.project.TrackInfo;
import ru.biosoft.bsa.transformer.SiteModelTransformer;
import ru.biosoft.bsa.view.BamTrackViewBuilder.BamTrackViewOptions;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.bsa.view.colorscheme.ConstantColorScheme;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;
import ru.biosoft.util.bean.StaticDescriptor;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

public class MakeMetaTracks extends AnalysisMethodSupport<MakeMetaTracks.Parameters>
{
    private String failedIteration = null;
    
    public MakeMetaTracks(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters());
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        List<TFDataSet> tfDataSets = loadTFDataSets();
        addSiteModels(tfDataSets);
        
        TaskPool.getInstance().iterate( tfDataSets, new ParallelIteration(), jobControl, parameters.getThreadsNumber() );
        if(failedIteration != null)
            throw new Exception("Failed at " + failedIteration);
        
        parameters.getTmpPath().remove();
        return parameters.getOutputPath().getDataCollection();
    }
    
    private List<TFDataSet> loadTFDataSets()
    {
        Map<String, TFDataSet> result = new TreeMap<>();
        DataCollection<ChIPseqExperiment> expColleciton = parameters.getExperiments().getDataCollection( ChIPseqExperiment.class );
        for( ChIPseqExperiment e : expColleciton )
        {
            if(e.isControlExperiment() || !e.getSpecie().getLatinName().equals( parameters.getSpecies().getLatinName() ))
                continue;
            
            if(e.getExpType() == ExperimentType.BIO_CONTROL)
                continue;
            
            String tf = e.getTfUniprotId();

            if( !result.containsKey( tf ) )
            {
                TFDataSet tfDataSet = new TFDataSet(tf);
                if( tfDataSet.addExperiment( e ) )
                    result.put( tf, tfDataSet );
            }
            else
                result.get( tf ).addExperiment( e );
        }
        Set<String> tfSet = new HashSet<>( Arrays.asList( parameters.getTfSubset() ) );
        return StreamEx.of( result.values() ).filter( d->tfSet.isEmpty() || tfSet.contains( d.getUniprotId() ) ).toList();
    }
    
    private void addSiteModels(List<TFDataSet> tfDataSets) throws Exception
    {
        for(SiteModelDC smDC : parameters.getSiteModels())
        {
            Map<String, String[]> tfClassToSM = fetchUniprotIdToSiteModels( smDC.getCollectionPath(), smDC.getName() );
            for(TFDataSet dataSet : tfDataSets)
            {
                String tf = dataSet.getUniprotId();
                String[] modelNames = tfClassToSM.get( tf );
                if(modelNames == null)
                {
                    log.info( "No site models for " + tf );
                    continue;
                }
                for(String smName : modelNames)
                {
                    SiteModel siteModel = smDC.getCollectionPath().getChildPath( smName ).getDataElement(SiteModel.class);
                    dataSet.addSiteModel( smDC.getName(), siteModel );
                }
            }
        }
    }
    
    
    private Map<String, String[]> fetchUniprotIdToSiteModels(DataElementPath siteModelCollectionPath, String name) throws Exception
    {
        DataElementPath siteModelsTablePath = parameters.getTmpPath().getChildPath( "siteModels." + name );
        
        TableDataCollection siteModelsTable = TableDataCollectionUtils.createTableDataCollection( siteModelsTablePath );
        siteModelCollectionPath.getChildren().stream()
            .map( p->new RowDataElement( p.getName(), siteModelsTable) )
            .forEach( siteModelsTable::put );
        siteModelsTablePath.save( siteModelsTable );
        
        DataElementPath outputPath = parameters.getTmpPath().getChildPath( "tfToSiteModel." + name );
        
        SiteModelsToProteins analysis = AnalysisMethodRegistry.getAnalysisMethod( SiteModelsToProteins.class );
        SiteModelsToProteinsParameters parameters = analysis.getParameters();
        parameters.setSitesCollection( siteModelsTablePath );
        parameters.setSiteModelsCollection( siteModelCollectionPath );
        parameters.setSpecies( this.parameters.getSpecies() );
        parameters.setTargetType( ReferenceTypeRegistry.getReferenceType( UniprotProteinTableType.class ).toString() );
        parameters.setOutputTable( outputPath );
        
        analysis.setLogger( log );
        runAnalysis( analysis );
        
        TableDataCollection outputTable = outputPath.getDataElement( TableDataCollection.class );
        Map<String, String[]> result = new HashMap<>();
        for(String tfClass : outputTable.getNameList())
        {
            String[] modelNames = outputTable.get( tfClass ).getValues()[0].toString().split( TableDataCollectionUtils.ID_SEPARATOR );
            result.put( tfClass, modelNames );
        }
        return result;
    }
    

    public class TFDataSet
    {
        private String uniprotId;
        private final List<ChIPseqExperiment> experiments = new ArrayList<>();
        private Map<String, List<SiteModel>> siteModels = new HashMap<>();
        
        public TFDataSet(String tf)
        {
            uniprotId = tf;
        }

        public String getUniprotId()
        {
            return uniprotId;
        }

        public Map<String, List<SiteModel>> getSiteModel()
        {
            return siteModels;
        }

        public List<ChIPseqExperiment> getExperiments()
        {
            return experiments;
        }
        
        boolean addExperiment(ChIPseqExperiment e)
        {
            if( e.isControlExperiment() )
                return false;
            String peaksId = e.getPeak().getName();
            boolean hasPeaks = parameters.getPeaksFolder()
                .getChildren().stream()
                .map( p->p.getChildPath( peaksId ) )
                .filter( p->p.exists() ).findFirst().isPresent();
            if( !hasPeaks )
                return false;
            experiments.add( e );
            return true;
        }
        
        void addSiteModel(String dc, SiteModel siteModel)
        {
            List<SiteModel> modelList = siteModels.get( dc );
            if(modelList == null)
                siteModels.put( dc, modelList = new ArrayList<>() );
            modelList.add( siteModel );
        }

    }
    
    
    
    //Stateless iteration suitable for parallel execution
    public class ParallelIteration implements Iteration<TFDataSet>
    {
        @Override
        public boolean run(TFDataSet dataSet)
        {
            return new TFIteration().run( dataSet );
        }
        
    }
    
    public class TFIteration implements Iteration<TFDataSet>
    {
        private final Map<String, PropertyDescriptor> DESCRIPTOR_CACHE = new HashMap<>();
        private final DecimalFormat DECIMAL_FORMAT = new DecimalFormat( "0.##E0" );    
        private static final int DEFAULT_BINDING_SITE_WIDTH = 20;
        private DataElementPath outPath;
        private TFDataSet dataSet;
        @Override
        public boolean run(TFDataSet dataSet)
        {
            log.info( "Processing " + dataSet.getUniprotId() );
            try
            {
                this.dataSet = dataSet;
                outPath = parameters.getOutputPath().getChildPath( dataSet.getUniprotId() );
                DataCollectionUtils.createFoldersForPath( outPath.getChildPath( "dummy" ) );
                
                List<CallerClusters> callerClusters = new ArrayList<>();
                Map<Caller, Track> callerPeaks = new HashMap<>();
                for(Caller peakCaller : Caller.values())
                {
                    List<ChIPseqExperiment> expList = new ArrayList<>();
                    List<Track> peaksList = new ArrayList<>();
                    for(ChIPseqExperiment e : dataSet.getExperiments())
                    {
                        String peakId = e.getPeak().getName();
                        DataElementPath peakPath = parameters.getPeaksFolder().getChildPath( peakCaller.name().toLowerCase(), peakId );
                        if(!peakPath.exists())
                            continue;
                        Track track = peakPath.getDataElement( Track.class );
                        peaksList.add( track );
                        expList.add( e );
                    }
                    if(expList.isEmpty())
                        continue;
                    
                    DataElementPath peaksPath = outPath.getChildPath( peakCaller + " peaks" );
                    Track peaks = catPeaks(peaksList, expList, peaksPath);
                    callerPeaks.put( peakCaller, peaks );
                    
                    DataElementPath clustersPath = outPath.getChildPath( peakCaller + " clusters" );
                    DataElementPath clusterToPeakPath = outPath.getChildPath( peakCaller + " cluster to peak" );
                    log.info( "Merging " + peakCaller.name() + " peaks from different experiments" );
                    mkClusters(peaks.getCompletePath(),  clustersPath, clusterToPeakPath);
                    callerClusters.add( new CallerClusters( peakCaller, clustersPath ));
                }
                SqlTrack clusters = mergeCallerClusters( callerClusters.toArray( new CallerClusters[0] ) );
                
                List<Track> allTracks = StreamEx.<Track>of( clusters )
                        .append( callerClusters.stream().map( c->c.getClustersPath().getDataElement( Track.class ) ) )
                        .append( callerPeaks.values() )
                        .toList();
                
                DataElementPath siteModelsPath = copySiteModels();
                Track motifTrack = null;
                if(!siteModelsPath.getDataCollection().isEmpty() && parameters.isRunSiteSearch())
                {
                    Track coverTrack = coverTracks(allTracks);
                    motifTrack = searchSites(coverTrack, siteModelsPath);
                }
                
                createView(outPath.getChildPath( "view" ), clusters, callerClusters, callerPeaks, motifTrack, dataSet);
                
                if( motifTrack != null && !parameters.isDisableMotifAnnotation() )
                {
                    addMotifs( clusters, motifTrack );
                    for( CallerClusters cc : callerClusters )
                        addMotifs( cc.getClustersPath().getDataElement( Track.class ), motifTrack );
                    for( Track t : callerPeaks.values() )
                        addMotifs( t, motifTrack );
                }
                
                //calculateDatasetStats();
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE,  "Error processing " + dataSet.getUniprotId(), t );
                failedIteration = dataSet.getUniprotId();
                return false;
            }
            log.info( "Finished " + dataSet.getUniprotId() );
            return true;
        }
        
        private void addMotifs(Track target, Track motifs)
        {
            log.info( "Adding motif information to " + target.getName() );
            DataElementPath resultPath = DataElementPath.create( target.getCompletePath().toString() + " with motifs" );
            SqlTrack result = SqlTrack.createTrack( resultPath, target );
            for(Site s : target.getAllSites())
            {
                SiteImpl sCopy = ((SiteImpl)s).clone( result );

                int summit;
                try{
                    summit = s.getFrom() + (int)s.getProperties().getValue( "summit" );
                }catch(Exception e)
                {
                    summit = s.getInterval().getCenter();
                }

                String seqName = s.getOriginalSequence().getCompletePath().toString();
                int queryFrom = Math.min(s.getFrom(), summit - 100);
                int queryTo = Math.max(s.getTo(), summit + 100);
                int i = 1;
                String closestSiteInfo = null;
                int closestDistance = Integer.MAX_VALUE;
                List<String> motifInfoList = new ArrayList<>();
                for(Site motifSite : motifs.getSites( seqName, queryFrom, queryTo ))
                {
                    Interval relativeInterval = motifSite.getInterval().shift( -summit );
                    int distance = Math.abs( relativeInterval.getCenter() );
                    SiteModel model = (SiteModel)motifSite.getProperties().getValue( SiteModel.SITE_MODEL_PROPERTY );
                    String value = i + " " + relativeInterval + " " + DECIMAL_FORMAT.format( motifSite.getScore() ) + " " + model.getName();
                    motifInfoList.add( value );
                    if(distance < closestDistance)
                    {
                        closestSiteInfo = value;
                        closestDistance = distance;
                    }
                    i++;
                }
                PropertyDescriptor descriptor = DESCRIPTOR_CACHE.computeIfAbsent( "motif", StaticDescriptor::create );
                sCopy.getProperties().add( new DynamicProperty( descriptor , String.class, TextUtil.joinTruncate( motifInfoList, 1000, "\n", "..." ) ) );
                if(closestSiteInfo != null)
                {
                    descriptor = DESCRIPTOR_CACHE.computeIfAbsent( "closestMotif", StaticDescriptor::create );
                    sCopy.getProperties().add( new DynamicProperty( descriptor, String.class, closestDistance + " " + closestSiteInfo ) );
                }
                
                result.addSite( sCopy );
            }
            result.finalizeAddition();
            resultPath.save( result );
        }

        private Track searchSites(Track coverTrack, DataElementPath siteModelsPath)
        {
            DataElementPath resultPath = outPath.getChildPath( "motifs" );
            if( !resultPath.exists() )
            {
                log.info( "Searching sites" );
                SiteSearchAnalysis analysis = AnalysisMethodRegistry.getAnalysisMethod( SiteSearchAnalysis.class );

                SiteSearchAnalysisParameters params = analysis.getParameters();
                params.setTrackPath( coverTrack.getCompletePath() );
                params.setProfilePath( siteModelsPath );

                params.setOutput( resultPath );

                runAnalysis( analysis );
            }
            
            return resultPath.getDataElement( Track.class );
        }

        private DataElementPath copySiteModels() throws Exception
        {
            DataElementPath result = outPath.getChildPath( "site models" );
            SiteModelCollection smc = SiteModelTransformer.createCollection(result);
            EntryStream.of( dataSet.getSiteModel() ).flatMapKeyValue( (db, modelList)->{
                return StreamEx.of( modelList ).map( model-> {
                    try
                    {
                        return model.clone( smc, db + "." + model.getName() );
                    }
                    catch( CloneNotSupportedException e )
                    {
                        throw ExceptionRegistry.translateException( e );
                    }
                } );
            } ).forEach( sm->smc.put( sm ) );
            result.save(smc);
            log.info( "Copy " + smc.getSize() + "  site models into " + result );
            return result;
        }

        private Track coverTracks(List<Track> allTracks)
        {
            DataElementPath resultPath = outPath.getChildPath( "cover" );
            if( !resultPath.exists() )
            {
                log.info( "Creating track for site search" );
                SqlTrack result = SqlTrack.createTrack( resultPath, allTracks.get( 0 ) );

                DataElementPath genomePath = TrackUtils.getTrackSequencesPath( allTracks.get( 0 ) );
                UnionTrack union = new UnionTrack( "", null, allTracks );
                Track merged = new MergedTrack( new ResizedTrack( union, "", 100, 100 ) );

                for( DataElementPath chrPath : genomePath.getChildren() )
                {
                    Sequence chrSeq = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
                    DataCollection<Site> sites = merged.getSites( chrPath.toString(), 0, chrSeq.getLength() + chrSeq.getStart() );
                    for( Site s : sites )
                        result.addSite( s );
                }
                result.finalizeAddition();
                resultPath.save( result );
            }
            return resultPath.getDataElement( Track.class );
        }

        private SqlTrack mergeCallerClusters(CallerClusters[] callerClusters)
        {
            DataElementPath resultPath = outPath.getChildPath( "meta clusters" );
            if( !resultPath.exists() )
            {
                log.info( "Merging peaks from distinct peak callers" );

                MergePeakCallers analysis = AnalysisMethodRegistry.getAnalysisMethod( MergePeakCallers.class );

                MergePeakCallers.Parameters params = analysis.getParameters();
                params.setCallerClusters( callerClusters );
                params.setOutputTrack( resultPath );
                params.setMetaClusterToClusterTable( outPath.getChildPath( "meta cluster to cluster" ) );

                runAnalysis( analysis );
            }
            
            return resultPath.getDataElement( SqlTrack.class );
        }

        private Track mkClusters(DataElementPath joinedPeaks, DataElementPath resultPath, DataElementPath clusterToPeakPath)
        {
            if( !resultPath.exists() )
            {
                MergePeaks analysis = AnalysisMethodRegistry.getAnalysisMethod( MergePeaks.class );
                MergePeaks.Parameters params = analysis.getParameters();

                int bsWidth = dataSet.getSiteModel().values().stream().flatMap( Collection::stream ).mapToInt( SiteModel::getLength ).max()
                        .orElse( DEFAULT_BINDING_SITE_WIDTH );
                params.setBindingSiteWidth( bsWidth );

                params.setInputTrack( joinedPeaks );
                params.setSdTable( DataElementPath.create( resultPath + " SD table" ) );
                params.setOutputTrack( resultPath );
                params.setClusterToPeakTable( clusterToPeakPath );
                
                runAnalysis( analysis );
            }
            
            return resultPath.getDataElement( Track.class );
        }

        private final StaticDescriptor EXP_DESCRIPTOR = StaticDescriptor.create( "exp" );
        private final StaticDescriptor PEAKS_DESCRIPTOR = StaticDescriptor.create( "peaks" );
        private final StaticDescriptor PEAK_ID_DESCRIPTOR = StaticDescriptor.create( "id" );
        private final StaticDescriptor CELL_DESCRIPTOR = StaticDescriptor.create( "cell" );
        private final StaticDescriptor TREATMENT_DESCRIPTOR = StaticDescriptor.create( "treatment" );
        private final StaticDescriptor ANTIBODY_DESCRIPTOR = StaticDescriptor.create( "antibody" );
        private final StaticDescriptor CONTROL_DESCRIPTOR = StaticDescriptor.create( "controlId" );
        
        private Track catPeaks(List<Track> peaksList, List<ChIPseqExperiment> expList, DataElementPath resultPath)
        {
            if( !resultPath.exists() )
            {
                log.info( "Concatenating " + peaksList.size() + " tracks" );
                SqlTrack result = SqlTrack.createTrack( resultPath, peaksList.get( 0 ) );
                for( int i = 0; i < peaksList.size(); i++ )
                {
                    Track peaks = peaksList.get( i );
                    ChIPseqExperiment e = expList.get( i );
                    for( Site s : peaks.getAllSites() )
                    {
                        Site copy = ( (SiteImpl)s ).clone( null );
                        DynamicPropertySet prop = copy.getProperties();
                        prop.add( new DynamicProperty( EXP_DESCRIPTOR, String.class, e.getName() ) );
                        prop.add( new DynamicProperty( PEAKS_DESCRIPTOR, String.class, e.getPeak().getName() ) );
                        prop.add( new DynamicProperty( PEAK_ID_DESCRIPTOR, String.class, e.getPeak().getName() + "." + s.getName() ) );
                        prop.add( new DynamicProperty( CELL_DESCRIPTOR, String.class, e.getCell().getTitle() ) );
                        prop.add( new DynamicProperty( TREATMENT_DESCRIPTOR, String.class, e.getTreatment() ) );
                        prop.add( new DynamicProperty( ANTIBODY_DESCRIPTOR, String.class, e.getAntibody() ) );
                        if( e.getControlId() != null )
                            prop.add( new DynamicProperty( CONTROL_DESCRIPTOR, String.class, e.getControlId() ) );
                        result.addSite( copy );
                    }
                }
                result.finalizeAddition();
                resultPath.save( result );
                sortTrack( resultPath );
                
            }
            return resultPath.getDataElement( Track.class );
        }
        
        private void sortTrack(DataElementPath trackPath)
        {
            SortSqlTrack method = AnalysisMethodRegistry.getAnalysisMethod( SortSqlTrack.class );
            SortSqlTrack.Parameters params = method.getParameters();
            
            params.setInputTrack( trackPath );
            params.setRegenerateIds( true );
            runAnalysis( method );
        }
    }
    
    private void runAnalysis(AnalysisMethod method) {
        method.setLogger( log );
        JobControl jc = method.getJobControl();
        jc.run();
        if(jc.getStatus() != JobControl.COMPLETED)
            throw new RuntimeException("Sub analysis failed: " + method.getName());
    }
    
    
    
    static void createView(DataElementPath resultPath, SqlTrack metaClusters, List<CallerClusters> callerClusters, Map<Caller, Track> callerPeaks, Track motifs, TFDataSet dataSet)
    {
        ProjectAsLists result = new ProjectAsLists( resultPath.getName(), resultPath.optParentCollection() );
        
        DataElementPath genomePath = TrackUtils.getTrackSequencesPath( metaClusters );
        String positionStr = metaClusters.getInfo().getProperties().getProperty( SqlTrack.DEFAULT_POSITION_PROPERTY );
        Position position; 
        if(positionStr != null)
        {
            position = new Position(positionStr);
        }
        else
        {
            String chrName = genomePath.getChildren().stream().findFirst()
                    .orElseThrow( () -> new RuntimeException( "No chromosomes found in " + genomePath ) )
                    .getName();
            position = new Position( chrName, new Interval(1, 2000) );
        }
        
        Region region = new Region( genomePath.getChildPath( position.getSequence() ).getDataElement(AnnotatedSequence.class) );
        region.setInterval( position.getInterval() );
        result.addRegion( region );
        
        result.addTrack( new TrackInfo( metaClusters )  );
        SiteViewOptions vo = result.getViewOptions().getTrackViewOptions( metaClusters.getCompletePath() );
        vo.setShowTitle( false );
        vo.setColorScheme( new ConstantColorScheme( new Color(255, 170, 170) ) );
        
        if(motifs != null)
        {
            result.addTrack( new TrackInfo( motifs ) );
            vo = result.getViewOptions().getTrackViewOptions( motifs.getCompletePath() );
            vo.setShowTitle( false );
        }
        
        EnumMap<Caller, Color> callerColors = new EnumMap<>( Caller.class );
        callerColors.put( Caller.GEM, new Color( 0, 191, 191 ) );
        callerColors.put( Caller.MACS2, new Color( 255, 0, 0 ) );
        callerColors.put( Caller.SISSRS, new Color(0, 191, 0) );
        callerColors.put( Caller.PICS, new Color(255, 170, 86) );
        
        for(CallerClusters c : callerClusters)
        {
            Track t = c.getClustersPath().getDataElement( Track.class );
            result.addTrack( new TrackInfo( t ) );
            vo = result.getViewOptions().getTrackViewOptions( t.getCompletePath() );
            vo.setShowTitle( false );
            vo.setColorScheme( new ConstantColorScheme( callerColors.getOrDefault( c.getCaller(), Color.BLUE ) ) );
        }
        callerPeaks.forEach( (c, t) -> {
            result.addTrack( new TrackInfo( t ) );
            SiteViewOptions svo = result.getViewOptions().getTrackViewOptions( t.getCompletePath() );
            svo.setShowTitle( false );
            Color col = callerColors.getOrDefault( c, Color.BLUE );
            svo.setColorScheme( new ConstantColorScheme(col) );
        } );
        
        for(ChIPseqExperiment exp : dataSet.getExperiments())
        {
            addAlignsView( result, exp );
        }
        for(ChIPseqExperiment exp : dataSet.getExperiments())
        {
        	String controlId = exp.getControlId();
            if(controlId != null)
            {
                if(controlId.substring(0, 4).equals("HEXP"))
                {
                	String pathToHexps = "databases/GTRD/Data/ChIP-seq HM experiments/";
                	HistonesExperiment ctrl = DataElementPath.create( pathToHexps + controlId).getDataElement( HistonesExperiment.class );
                	addAlignsView( result, ctrl );
                }
                else
                {
                	ChIPseqExperiment ctrl = exp.getControl().getDataElement( ChIPseqExperiment.class );                	
                	addAlignsView( result, ctrl );
                }
            }
                
        }
        
        resultPath.save( result );
    }

    private static void addAlignsView(ProjectAsLists result, ChIPseqExperiment exp) throws RepositoryException
    {
        DataElementPath aling = exp.getAlignment();
        if(aling.exists())
        {
            Track track = aling.getDataElement( Track.class );
            TrackInfo ti = new TrackInfo( track );
            String title = exp.getName() + " ("+track.getName()+")";
            if(exp.isControlExperiment())
                title = "Ctrl " + title;
            ti.setTitle( title );
            result.addTrack( ti );
            BamTrackViewOptions viewOptions = (BamTrackViewOptions)result.getViewOptions().getTrackViewOptions( aling );
            viewOptions.setProfileView( true );
        }
    }
    
    private static void addAlignsView(ProjectAsLists result, HistonesExperiment exp) throws RepositoryException
    {
        DataElementPath aling = exp.getAlignment();
        if(aling.exists())
        {
            Track track = aling.getDataElement( Track.class );
            TrackInfo ti = new TrackInfo( track );
            String title = exp.getName() + " ("+track.getName()+")";
            if(exp.isControlExperiment())
                title = "Ctrl " + title;
            ti.setTitle( title );
            result.addTrack( ti );
            BamTrackViewOptions viewOptions = (BamTrackViewOptions)result.getViewOptions().getTrackViewOptions( aling );
            viewOptions.setProfileView( true );
        }
    }
    

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath experiments;
        public DataElementPath getExperiments()
        {
            return experiments;
        }
        public void setExperiments(DataElementPath experiments)
        {
            Object oldValue = this.experiments;
            this.experiments = experiments;
            firePropertyChange( "experiments", oldValue, experiments );
        }
        
        private String[] tfSubset = new String[0];
        public String[] getTfSubset()
        {
            return tfSubset;
        }
        public void setTfSubset(String[] tfSubset)
        {
            Object oldValue = this.tfSubset;
            this.tfSubset = tfSubset;
            firePropertyChange( "tfSubset", oldValue, tfSubset );
        }

        private Species species;
        public Species getSpecies()
        {
            return species;
        }
        public void setSpecies(Species species)
        {
            Object oldValue = this.species;
            this.species = species;
            firePropertyChange( "species", oldValue, species );
        }
        
        private DataElementPath peaksFolder;
        public DataElementPath getPeaksFolder()
        {
            return peaksFolder;
        }
        public void setPeaksFolder(DataElementPath peaksFolder)
        {
            Object oldValue = this.peaksFolder;
            this.peaksFolder = peaksFolder;
            firePropertyChange( "peaksFolder", oldValue, peaksFolder );
        }
        
        private SiteModelDC[] siteModels = new SiteModelDC[0];
        public SiteModelDC[] getSiteModels()
        {
            return siteModels;
        }
        public void setSiteModels(SiteModelDC[] siteModels)
        {
            Object oldValue = this.siteModels;
            this.siteModels = siteModels;
            firePropertyChange( "siteModels", oldValue, siteModels );
        }

        private boolean runSiteSearch = true;
        public boolean isRunSiteSearch()
        {
            return runSiteSearch;
        }
        public void setRunSiteSearch(boolean runSiteSearch)
        {
            boolean oldValue = this.runSiteSearch;
            this.runSiteSearch = runSiteSearch;
            firePropertyChange( "runSiteSearch", oldValue, runSiteSearch );
        }
        
        private boolean disableMotifAnnotation = false;
        public boolean isDisableMotifAnnotation()
        {
            return disableMotifAnnotation;
        }
        public void setDisableMotifAnnotation(boolean disableMotifAnnotation)
        {
            boolean oldValue = this.disableMotifAnnotation;
            this.disableMotifAnnotation = disableMotifAnnotation;
            firePropertyChange( "disableMotifAnnotation", oldValue, disableMotifAnnotation );
        }
        
        private int threadsNumber = 1;
        public int getThreadsNumber()
        {
            return threadsNumber;
        }
        public void setThreadsNumber(int threadsNumber)
        {
            int oldValue = this.threadsNumber;
            this.threadsNumber = threadsNumber;
            firePropertyChange( "threadsNumber", oldValue, threadsNumber );
        }

        private DataElementPath outputPath;
        public DataElementPath getOutputPath()
        {
            return outputPath;
        }
        public void setOutputPath(DataElementPath outputPath)
        {
            Object oldValue = this.outputPath;
            this.outputPath = outputPath;
            firePropertyChange( "outputPath", oldValue, outputPath );
        }
        
        DataElementPath getTmpPath()
        {
            DataElementPath res = getOutputPath().getChildPath( "tmp" );
            if(!res.exists())
                DataCollectionUtils.createFoldersForPath( res.getChildPath( "child" ) );
            return res;
        }
    }
    

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( DataElementPathEditor.registerInputChild( "experiments", beanClass, ChIPseqExperiment.class ) );
            add( DataElementComboBoxSelector.registerSelector( "species", beanClass, Species.SPECIES_PATH ) );
            property( "tfSubset" ).editor( TFMultiSelector.class ).hideChildren().add();
            property("peaksFolder").inputElement( FolderCollection.class ).add();
            add("siteModels");
            add("runSiteSearch");
            add("disableMotifAnnotation");
            add("threadsNumber");
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
    
    public static class SiteModelDC extends OptionEx implements JSONBean
    {
        private String name;

        public String getName()
        {
            return name;
        }
        public void setName(String name)
        {
            Object oldValue = this.name;
            this.name = name;
            firePropertyChange( "name", oldValue, name );
        }
        
        private DataElementPath collectionPath;
        public DataElementPath getCollectionPath()
        {
            return collectionPath;
        }
        public void setCollectionPath(DataElementPath collectionPath)
        {
            Object oldValue = this.collectionPath;
            this.collectionPath = collectionPath;
            firePropertyChange( "collectionPath", oldValue, collectionPath );
        }
    }
    public static class SiteModelDCBeanInfo extends BeanInfoEx2<SiteModelDC>
    {
        public SiteModelDCBeanInfo()
        {
            super( SiteModelDC.class );
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("name");
            property("collectionPath").inputElement( SiteModelCollection.class ).add();
        }
    }
    

    public static class TFMultiSelector extends GenericMultiSelectEditor
    {   
        @Override
        protected String[] getAvailableValues()
        {
            Parameters params = (Parameters)getBean();
            DataElementPath experiments = params.getExperiments();
            Species species = params.getSpecies();
            if(experiments == null || species == null)
                return new String[0];
            Connection con = DataCollectionUtils.getSqlConnection( experiments.getDataElement() );
            String query = "SELECT DISTINCT tf_uniprot_id FROM chip_experiments WHERE NOT(isNULL(tf_uniprot_id)) AND specie=" + SqlUtil.quoteString( species.getLatinName() ) + " ORDER BY 1";
            return SqlUtil.queryStrings( con, query ).toArray(new String[0]);
        }
    }
}

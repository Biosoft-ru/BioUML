package ru.biosoft.bsa.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.standard.type.Base;
import biouml.standard.type.Species;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.BioHubRegistry;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.SiteModelsToProteinsSupport.Link;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.journal.ProjectUtils;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.table.exception.TableNoColumnException;
import ru.biosoft.util.Pair;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;

public class FindFeedForwardLoops extends AnalysisMethodSupport<FindFeedForwardLoops.Parameters>
{
    public FindFeedForwardLoops(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info( "Loading TFs enriched in miRNA promoters" );
        jobControl.pushProgress( 0, 5 );
        TableDataCollection miRNATFTable = parameters.getMiRNATFTable().getDataElement(TableDataCollection.class);
        Map<String, Double> miRNATFs = loadEnrichedTFs( miRNATFTable );//transcription factors enriched in promoters of miRNA
        jobControl.popProgress();
        
        log.info( "Loading TFs enriched in promoters of target genes" );
        jobControl.pushProgress( 5, 10 );
        TableDataCollection targetTFTable = parameters.getTargetTFTable().getDataElement( TableDataCollection.class );
        Map<String, Double> targetTFs = loadEnrichedTFs( targetTFTable );//transcription factors enriched in promoters of target genes (genes that are targets of miRNA)
        jobControl.popProgress();

        //we are only interested in TFs that regulate both miRNA and their targets
        miRNATFs.keySet().retainAll( targetTFs.keySet() );
        targetTFs.keySet().retainAll( miRNATFs.keySet() );
        log.info( "Found " + miRNATFs.size() + " TF(s) enriched in both miRNA and target gene promoters" );
        
        log.info( "Computing score of miRNA promoters" );
        jobControl.pushProgress( 10, 50 );
        Map<String, Map<String, PromoterScore>> miRNAScores = computeMiRNAScores( miRNATFs );
        jobControl.popProgress();
        
        log.info( "Loading miRNA targets table" );
        jobControl.pushProgress( 50, 55 );
        List<MiRNATargetScore> miRNATargetScores = loadTargetToMiRNA();
        Map<String, List<String>> matureToStemLoop = loadMatureToStemLoop();
        miRNATargetScores = convertToStemLoop(miRNATargetScores, matureToStemLoop);
        jobControl.popProgress();

        log.info( "Searching feed forward loops and computing scores" );
        jobControl.pushProgress( 55, 90 );
        Map<FeedForwardLoop, FFLScore> fflScores = computeFFLScores( targetTFs, miRNAScores, miRNATargetScores );
        jobControl.popProgress();
        
        log.info( "Annotating results" );
        jobControl.pushProgress( 90, 95 );
        Map<String, String[]> siteModelsToIsogroups = mapSiteModelsToTransfacIsogroups( fflScores.keySet() );
        fflScores = mapResultsToIsogroups(fflScores, siteModelsToIsogroups);
        annotateGeneSymbols(fflScores.keySet());
        annotateFactorNames(fflScores.keySet()); 
        jobControl.popAndShrink();
        
        log.info( "Writing results" );
        jobControl.pushProgress( 95, 100 );
        TableDataCollection res = writeResults( fflScores );
        jobControl.popProgress();
        return res;
    }

    private Map<String, List<String>> loadMatureToStemLoop()
    {
        Map<String, List<String>> result = new HashMap<>();
        TableDataCollection table = parameters.getStemLoopToMatureTable().getDataElement( TableDataCollection.class );
        for(RowDataElement rde : table)
        {
            String stemLoop = rde.getName();
            String[] matureList = TextUtil.split( rde.getValues()[0].toString(), ',');
            for(String mature : matureList)
                result.computeIfAbsent( mature, k->new ArrayList<>() ).add( stemLoop );
        }
        return result;
    }

    private List<MiRNATargetScore> convertToStemLoop(List<MiRNATargetScore> miRNATargetScores, Map<String, List<String>> matureToStemLoop)
    {
        Map<Pair<String,String>, Double> uniqueResults = new HashMap<>();
        for(MiRNATargetScore e : miRNATargetScores)
        {
            String mature = e.miRNA;
            List<String> stemLoops = matureToStemLoop.get( mature );
            if(stemLoops != null)
                for(String stemLoop : stemLoops)
                {
                    uniqueResults.compute( new Pair<>( stemLoop, e.target ), (k, oldScore)->oldScore == null ? e.score : Math.max( oldScore, e.score ) );
                }
        }
        return uniqueResults.entrySet().stream()
                .map( e->new MiRNATargetScore( e.getKey().getFirst(), e.getKey().getSecond(), e.getValue() ) )
                .collect( Collectors.toList() );
    }

    private TableDataCollection writeResults(Map<FeedForwardLoop, FFLScore> fflScores) throws LoggedException, DataElementPutException
    {
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getOutTable() );
        ColumnModel cm = result.getColumnModel();
        cm.addColumn( "miRNA", DataType.Text );
        cm.addColumn( "Target gene", DataType.Text );
        cm.addColumn( "Target gene name", DataType.Text );
        cm.addColumn( "Transcription factor", DataType.Text );
        cm.addColumn( "Site model", DataType.Text );
        cm.addColumn( "Score", DataType.Float );
        cm.addColumn( "miRNA promoter score", DataType.Float );
        cm.addColumn( "Target gene promoter score", DataType.Float );
        cm.addColumn( "miRNA target score", DataType.Float );
        cm.addColumn( "miRNA promoter site density", DataType.Float );
        cm.addColumn( "No density (miRNA)", DataType.Float );
        cm.addColumn( "Target gene promoter site density", DataType.Float );
        cm.addColumn( "No density (target genes)", DataType.Float );

        AtomicInteger fflId = new AtomicInteger( 1 );
        fflScores.entrySet().stream().sorted( (a,b)->Double.compare( b.getValue().getScore(), a.getValue().getScore() ) )
        .forEach( entry -> {
            FeedForwardLoop ffl = entry.getKey();
            FFLScore score = entry.getValue();
            String rowName = String.valueOf( fflId.getAndIncrement() );
            String geneSymbol = ffl.props.get( FeedForwardLoop.PROP_GENE_SYMBOL );
            String tfName = ffl.props.get( FeedForwardLoop.PROP_TF_TITLE );
            String siteModel = ffl.props.get( FeedForwardLoop.PROP_SITE_MODEL );
            Object[] values = new Object[] {ffl.miRNA, ffl.gene, geneSymbol, tfName, siteModel, score.getScore(), 
                    score.getMiRNAScore().getScore(), score.getTargetGeneScore().getScore(), score.getMiRNATargetScore(),
                    score.getMiRNAScore().getPromoterDensity(), score.getMiRNAScore().getNoDensity(),
                    score.getTargetGeneScore().getPromoterDensity(), score.getTargetGeneScore().getNoDensity()};
            TableDataCollectionUtils.addRow( result, rowName, values, true );
        } );
        result.finalizeAddition();
        
        TableDataCollectionUtils.setSortOrder( result, "Score", false );
        parameters.getOutTable().save( result );
        return result;
    }
    
    private void annotateGeneSymbols(Set<FeedForwardLoop> ffls)
    {
        String[] geneIds = ffls.stream().map( ffl->ffl.gene ).distinct().toArray( String[]::new );
        
        Properties input = new Properties();
        input.setProperty( BioHub.TYPE_PROPERTY, "Genes: Ensembl" );
        input.setProperty( BioHub.SPECIES_PROPERTY, "Homo sapiens" );
        
        Properties output = new Properties();
        output.setProperty( BioHub.TYPE_PROPERTY, "Genes: Gene symbol" );
        output.setProperty( BioHub.SPECIES_PROPERTY, "Homo sapiens" );
        
        Map<String, String[]> mapping = BioHubRegistry.getReferences( geneIds, input, output, null );
        for(FeedForwardLoop ffl : ffls)
        {
            String[] symbols = mapping.get( ffl.gene );
            String symbolsJoined = String.join( ", ", symbols );
            ffl.props.put( FeedForwardLoop.PROP_GENE_SYMBOL, symbolsJoined );
        }
    }
    
    private void annotateFactorNames(Set<FeedForwardLoop> ffls) throws Exception
    {
        DataElementPath transpath = ProjectUtils.getPreferredDatabasePath( "Transpath",
                ProjectUtils.getDestinationProjectPath( parameters.getOutTable() ) );
        DataCollection<Base> molCollection = transpath.getChildPath( "Data", "molecule" ).getDataCollection( Base.class );
        
        Set<String> mols = ffls.stream().map( ffl->ffl.tf ).collect( Collectors.toSet() );
        
        Map<String, String> mapping = new HashMap<>();
        for(String mol : mols)
            mapping.put( mol, molCollection.get( mol ).getTitle() );
        
        for(FeedForwardLoop ffl : ffls)
            ffl.props.put( FeedForwardLoop.PROP_TF_TITLE, mapping.get( ffl.tf ) );
    }


    
    private Map<String, String[]> mapSiteModelsToTransfacIsogroups(Set<FeedForwardLoop> ffls) throws Exception
    {
        String libraryPath = parameters.getMiRNATFSites().getDataCollection().getInfo().getProperty( "DataCollection_siteModel" );
        DataCollection<SiteModel> siteLibrary = DataElementPath.create( libraryPath ).getDataCollection( SiteModel.class );

        String[] siteModelNames = ffls.stream().map( ffl->ffl.tf ).distinct().toArray( String[]::new );
        
        Species species = Species.getSpecies( "Homo sapiens" );

        jobControl.pushProgress( 0, 50 );
        Map<String, Set<Link>> factors = SiteModelsToProteins.getFactors( siteLibrary, siteModelNames, species, jobControl, log );
        jobControl.popProgress();
        
        jobControl.pushProgress( 50, 100 );
        Map<String, String[]> molecules = SiteModelsToProteins.getMolecules( factors, ReferenceTypeRegistry.getReferenceType( "Proteins: Transpath isogroups" ), species, jobControl );
        jobControl.popProgress();
        
        return molecules;
    }
    
    private Map<FeedForwardLoop, FFLScore> mapResultsToIsogroups(Map<FeedForwardLoop, FFLScore> fflScores,
            Map<String, String[]> siteModelsToFactors)
    {
        Map<FeedForwardLoop, FFLScore> result = new HashMap<>();
        fflScores.forEach( (ffl, score)->{
            String siteModel = ffl.tf;
            for(String tf : siteModelsToFactors.get( siteModel ))
            {
                FeedForwardLoop newFFL = new FeedForwardLoop( ffl.miRNA, tf, ffl.gene );
                newFFL.props.put( FeedForwardLoop.PROP_SITE_MODEL, siteModel );
                result.compute( newFFL, (k,v)->{
                    if(v==null || score.getScore() > v.getScore())
                        return score;
                    return v;
                } );
            }
        } );
        return result;
    }

    private Map<FeedForwardLoop, FFLScore> computeFFLScores(Map<String, Double> targetTFs, Map<String, Map<String, PromoterScore>> miRNAPromoterScores,
            List<MiRNATargetScore> miRNATargetScores)
    {
        Map<FeedForwardLoop, FFLScore> fflScores = new HashMap<>();
        Track targetPromoters = parameters.getTargetPromoters().getDataElement( Track.class );
        Track targetTFSites = parameters.getTargetTFSites().getDataElement( Track.class );
        
        for(Site promoter : targetPromoters.getAllSites())
        {
            String gene = promoter.getProperties().getValueAsString( "id" );
            for(MiRNATargetScore mts : miRNATargetScores)
            {
                if(!mts.target.equals( gene ))
                    continue;
                Map<String, PromoterScore> miRNAScoresByTF = miRNAPromoterScores.get( mts.miRNA );
                if(miRNAScoresByTF == null)//possibly promoter was not known for this miRNA
                    continue;
                
                Map<String, Integer> tfIndex = indexSet( miRNAScoresByTF.keySet() );
                int[] bsCount = new int[tfIndex.size()];
                
                String chr = promoter.getOriginalSequence().getCompletePath().toString();
                DataCollection<Site> bindingSites = targetTFSites.getSites( chr, promoter.getFrom(), promoter.getTo() );
                for(Site bs : bindingSites)
                {
                    DynamicPropertySet properties = bs.getProperties();
                    String tf = properties.getValueAsString( SiteModel.SITE_MODEL_PROPERTY );
                    Integer tfIdx = tfIndex.get( tf );
                    if(tfIdx == null)
                        continue;
                    bsCount[tfIdx]++;
                }
                
                for(Map.Entry<String, PromoterScore> e : miRNAScoresByTF.entrySet())
                {
                    String tf = e.getKey();
                    PromoterScore miRNAScore = e.getValue();
                    
                    double promoterDensity = (1000. * bsCount[tfIndex.get( tf )] ) / promoter.getLength();
                    double noDensity = targetTFs.get( tf );
                    PromoterScore targetScore = new PromoterScore( promoterDensity, noDensity );
                    
                    if( targetScore.getScore() > 0 )
                    {
                        FFLScore fflScore = new FFLScore( miRNAScore, targetScore, mts.score ); 
                        FeedForwardLoop key = new FeedForwardLoop( mts.miRNA, tf, gene );
                        fflScores.compute( key, (k, oldScore) -> {
                            if(oldScore == null)
                                return fflScore;
                            if(fflScore.getScore() > oldScore.getScore())
                                return fflScore;
                            else
                                return oldScore;
                        } );
                    }
                }
            }
        }
        
        return fflScores;
    }


    private Map<String, Map<String, PromoterScore>> computeMiRNAScores(Map<String, Double> miRNATFs)
            throws RepositoryException, UnsupportedOperationException
    {
        Track miRNAPromoters = parameters.getMiRNAPromoters().getDataElement( Track.class );
        Track miRNATFSites = parameters.getMiRNATFSites().getDataElement( Track.class );//TF binding sites in promoters of miRNA
        
        Map<String, Integer> tfIndex = indexSet( miRNATFs.keySet() );

        Map<String, Map<String, PromoterScore>> miRNAScores = new HashMap<>();//[miRNA][tf]->score
        for(Site promoter : miRNAPromoters.getAllSites())
        {
            String miRNA = promoter.getProperties().getValueAsString( "miRNA" );
            
            //multiple promoters per one miRNA possible
            Map<String, PromoterScore> scoresByTF = miRNAScores.computeIfAbsent( miRNA, k->new HashMap<>() );
            
            String chr = promoter.getOriginalSequence().getCompletePath().toString();
            DataCollection<Site> bindingSites = miRNATFSites.getSites( chr, promoter.getFrom(), promoter.getTo() );
            int[] bsCount = new int[tfIndex.size()];
            for(Site bs : bindingSites) {
                DynamicPropertySet properties = bs.getProperties();
                String tf = properties.getValueAsString( SiteModel.SITE_MODEL_PROPERTY );
                Integer tfIdx = tfIndex.get( tf );
                if(tfIdx == null)
                    continue;
                bsCount[tfIdx]++;
            }

            tfIndex.forEach( (tf, idx) -> {
                Double noDensity = miRNATFs.get( tf );
                double promoterDensity = (1000. * bsCount[idx]) / promoter.getLength();
                PromoterScore promoterScore = new PromoterScore( promoterDensity, noDensity );
                if(promoterScore.getScore() > 0)
                    //Select promoter with best score sum                    
                    scoresByTF.compute( tf, (k, oldScore) -> {
                        if(oldScore == null)
                            return promoterScore;
                        if( promoterScore.getScore() > oldScore.getScore() )
                            return promoterScore;
                        else
                            return oldScore;
                    } );
            } );
        }
        return miRNAScores;
    }
    
    private static Map<String, Integer> indexSet(Set<String> s)
    {
        Map<String, Integer> result = new HashMap<>();
        for(String key : s)
            result.put( key, result.size() );
        return result;
    }
    
    
    private List<MiRNATargetScore> loadTargetToMiRNA()
    {
        List<MiRNATargetScore> result = new ArrayList<>();
        TableDataCollection table = parameters.getMiRNATargetScores().getDataElement( TableDataCollection.class );
        int miRNAColIdx = table.getColumnModel().getColumnIndex( "miRNA" );
        int targetColIdx = table.getColumnModel().getColumnIndex( "Target gene" );
        int scoreColIdx = table.getColumnModel().getColumnIndex( "Score" );
        for(RowDataElement row : table)
        {
            Object[] values = row.getValues();
            String miRNA = (String)values[miRNAColIdx];
            String target = (String)values[targetColIdx];
            double score = ((Number)values[scoreColIdx]).doubleValue();
            result.add( new MiRNATargetScore( miRNA, target, score ) );
        }
        return result;
    }
    
    private Map<String, Double> loadEnrichedTFs(TableDataCollection siteSearchSummaryTable) throws TableNoColumnException
    {
        int pValueColumn = siteSearchSummaryTable.getColumnModel().getColumnIndex( "P-value" );
        int noDensityColumn = siteSearchSummaryTable.getColumnModel().getColumnIndex( "No density per 1000bp" );
        Map<String, Double> tfs = new HashMap<>( );
        for(RowDataElement row : siteSearchSummaryTable)
        {
            String tf = row.getName();
            Object[] values = row.getValues();
            double pValue = ((Number)values[pValueColumn]).doubleValue();
            if(pValue <= parameters.getTfEnrichmentPValue())
            {
                double noDensity = ((Number)values[noDensityColumn]).doubleValue();
                tfs.put( tf, noDensity );
            }
            
        }
        return tfs;
    }
    
    private static class MiRNATargetScore
    {
        public final String miRNA;
        public final String target;
        public final double score;
        public MiRNATargetScore(String miRNA, String target, double score)
        {
            this.miRNA = miRNA;
            this.target = target;
            this.score = score;
        }
    }

    private static class FeedForwardLoop
    {
        

        //TF regulates miRNA and gene, gene is the target of miRNA
        String miRNA, tf, gene;
        
        public static final String PROP_SITE_MODEL = "SiteModel";
        public static final String PROP_GENE_SYMBOL = "GeneSymbol";
        public static final String PROP_TF_TITLE = "TFTitle";
        Map<String, String> props = new HashMap<>();

        public FeedForwardLoop(String miRNA, String tf, String gene)
        {
            this.miRNA = miRNA;
            this.tf = tf;
            this.gene = gene;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( gene == null ) ? 0 : gene.hashCode() );
            result = prime * result + ( ( miRNA == null ) ? 0 : miRNA.hashCode() );
            result = prime * result + ( ( tf == null ) ? 0 : tf.hashCode() );
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if( this == obj )
                return true;
            if( obj == null )
                return false;
            if( getClass() != obj.getClass() )
                return false;
            FeedForwardLoop other = (FeedForwardLoop)obj;
            if( gene == null )
            {
                if( other.gene != null )
                    return false;
            }
            else if( !gene.equals( other.gene ) )
                return false;
            if( miRNA == null )
            {
                if( other.miRNA != null )
                    return false;
            }
            else if( !miRNA.equals( other.miRNA ) )
                return false;
            if( tf == null )
            {
                if( other.tf != null )
                    return false;
            }
            else if( !tf.equals( other.tf ) )
                return false;
            return true;
        }

        
    }
    
    private static class PromoterScore
    {
        private double promoterDensity;
        private double noDensity;
        private double score;
        
        public PromoterScore(double promoterDensity, double noDensity)
        {
            this.promoterDensity = promoterDensity;
            this.noDensity = noDensity;
            this.score = promoterDensity/noDensity;
        }


        public double getPromoterDensity()
        {
            return promoterDensity;
        }

        public double getNoDensity()
        {
            return noDensity;
        }

        public double getScore()
        {
            return score;
        }
    }
    
    private static class FFLScore
    {
        private PromoterScore miRNAScore;
        private PromoterScore targetGeneScore;
        private double miRNATargetScore;
        private double score;
        
        public FFLScore(PromoterScore miRNAScore, PromoterScore targetGeneScore, double miRNATargetScore)
        {
            this.miRNAScore = miRNAScore;
            this.targetGeneScore = targetGeneScore;
            this.miRNATargetScore = miRNATargetScore;
            this.score = miRNAScore.getScore() + targetGeneScore.getScore() + miRNATargetScore;
        }

        public PromoterScore getMiRNAScore()
        {
            return miRNAScore;
        }

        public PromoterScore getTargetGeneScore()
        {
            return targetGeneScore;
        }
        
        public double getMiRNATargetScore()
        {
            return miRNATargetScore;
        }

        public double getScore()
        {
            return score;
        }
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath miRNATFTable, miRNATFSites, miRNAPromoters;
        private DataElementPath targetTFTable, targetTFSites, targetPromoters;
        private DataElementPath miRNATargetScores;
        private DataElementPath stemLoopToMatureTable;
        private DataElementPath outTable;
        private double tfEnrichmentPValue = 0.01;
        
        public DataElementPath getMiRNATFTable()
        {
            return miRNATFTable;
        }
        public void setMiRNATFTable(DataElementPath miRNATFTable)
        {
            Object oldValue = this.miRNATFTable;
            this.miRNATFTable = miRNATFTable;
            firePropertyChange( "miRNATFTable", oldValue, miRNATFTable );
        }
        
        public DataElementPath getMiRNATFSites()
        {
            return miRNATFSites;
        }
        public void setMiRNATFSites(DataElementPath miRNATFSites)
        {
            Object oldValue = this.miRNATFSites;
            this.miRNATFSites = miRNATFSites;
            firePropertyChange( "miRNATFSites", oldValue, miRNATFSites );
        }
        
        public DataElementPath getMiRNAPromoters()
        {
            return miRNAPromoters;
        }
        public void setMiRNAPromoters(DataElementPath miRNAPromoters)
        {
            Object oldValue = this.miRNAPromoters;
            this.miRNAPromoters = miRNAPromoters;
            firePropertyChange( "miRNAPromoters", oldValue, miRNAPromoters );
        }
        
        public DataElementPath getTargetTFTable()
        {
            return targetTFTable;
        }
        public void setTargetTFTable(DataElementPath targetTFTable)
        {
            Object oldValue = this.targetTFTable;
            this.targetTFTable = targetTFTable;
            firePropertyChange( "targetTFTable", oldValue, targetTFTable );
        }
        
        public DataElementPath getTargetTFSites()
        {
            return targetTFSites;
        }
        public void setTargetTFSites(DataElementPath targetTFSites)
        {
            Object oldValue = this.targetTFSites;
            this.targetTFSites = targetTFSites;
            firePropertyChange( "targetTFSites", oldValue, targetTFSites );
        }
        
        public DataElementPath getTargetPromoters()
        {
            return targetPromoters;
        }
        public void setTargetPromoters(DataElementPath targetPromoters)
        {
            Object oldValue = this.targetPromoters;
            this.targetPromoters = targetPromoters;
            firePropertyChange( "targetPromoters", oldValue, targetPromoters );
        }
        
        public DataElementPath getMiRNATargetScores()
        {
            return miRNATargetScores;
        }
        public void setMiRNATargetScores(DataElementPath miRNATargets)
        {
            Object oldValue = this.miRNATargetScores;
            this.miRNATargetScores = miRNATargets;
            firePropertyChange( "miRNATargetScores", oldValue, miRNATargets );
        }
        
        public DataElementPath getStemLoopToMatureTable()
        {
            return stemLoopToMatureTable;
        }
        public void setStemLoopToMatureTable(DataElementPath stemLoopToMatureTable)
        {
            Object oldValue = this.stemLoopToMatureTable;
            this.stemLoopToMatureTable = stemLoopToMatureTable;
            firePropertyChange( "stemLoopToMatureTable", oldValue, stemLoopToMatureTable );
        }
        
        public double getTfEnrichmentPValue()
        {
            return tfEnrichmentPValue;
        }
        public void setTfEnrichmentPValue(double tfEnrichmentPValue)
        {
            double oldValue = this.tfEnrichmentPValue;
            this.tfEnrichmentPValue = tfEnrichmentPValue;
            firePropertyChange( "tfEnrichmentPValue", oldValue, tfEnrichmentPValue );
        }
        
        public DataElementPath getOutTable()
        {
            return outTable;
        }
        public void setOutTable(DataElementPath outTable)
        {
            Object oldValue = this.outTable;
            this.outTable = outTable;
            firePropertyChange( "outTable", oldValue, outTable );
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
            property("miRNATFTable").inputElement( TableDataCollection.class ).add();
            property("miRNAPromoters").inputElement( Track.class ).add();
            property("miRNATFSites").inputElement( Track.class ).add();
            property("targetTFTable").inputElement( TableDataCollection.class ).add();
            property("targetPromoters").inputElement( Track.class ).add();
            property("targetTFSites").inputElement( Track.class ).add();
            property("miRNATargetScores").inputElement( TableDataCollection.class ).add();
            property("stemLoopToMatureTable").inputElement( TableDataCollection.class ).add();
            add("tfEnrichmentPValue");
            property("outTable").outputElement( TableDataCollection.class ).add();
        }
    }
}

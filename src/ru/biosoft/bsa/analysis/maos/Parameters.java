package ru.biosoft.bsa.analysis.maos;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.SiteModelUtils;
import ru.biosoft.bsa.Track;

@SuppressWarnings ( "serial" )
public class Parameters extends AbstractAnalysisParameters
{
    private DataElementPath vcfTrack;
    private BasicGenomeSelector genome;
    private DataElementPath siteModels;
    private double scoreDiff = 5;
    private DataElementPath siteGainTrack;
    private DataElementPath siteLossTrack;
    private DataElementPath summaryTable;

    public Parameters()
    {
        setGenome( new BasicGenomeSelector() );
        setSiteModels( SiteModelUtils.getDefaultProfile() );
    }

    @PropertyName("Input VCF track")
    @PropertyDescription("Input track in VCF format")
    public DataElementPath getVcfTrack()
    {
        return vcfTrack;
    }
    public void setVcfTrack(DataElementPath vcfTrack)
    {
        Object oldValue = this.vcfTrack;
        this.vcfTrack = vcfTrack;
        if( vcfTrack != null )
            genome.setFromTrack( vcfTrack.getDataElement( Track.class ) );
        firePropertyChange( "vcfTrack", oldValue, vcfTrack );
    }
    public Track getVcfTrackDataElement()
    {
        return getVcfTrack().getDataElement( Track.class );
    }

    @PropertyName("Genome")
    @PropertyDescription("Reference genome")
    public BasicGenomeSelector getGenome()
    {
        return genome;
    }
    public void setGenome(BasicGenomeSelector genome)
    {
        Object oldValue = this.genome;
        this.genome = genome;
        genome.setParent( this );
        firePropertyChange( "genome", oldValue, genome );
    }
    public DataElementPath getChromosomesPath()
    {
        return getGenome().getSequenceCollectionPath();
    }
    public DataElementPath getEnsemblPath()
    {
        return getGenome().getDbSelector().getBasePath();
    }

    @PropertyName("Profile")
    @PropertyDescription("Predefined set of site models")
    public DataElementPath getSiteModels()
    {
        return siteModels;
    }
    public void setSiteModels(DataElementPath siteModels)
    {
        Object oldValue = this.siteModels;
        this.siteModels = siteModels;
        firePropertyChange( "siteModels", oldValue, siteModels );
    }
    public SiteModelCollection getSiteModelCollection()
    {
        return getSiteModels().getDataElement( SiteModelCollection.class );
    }
    
    public static final String SITE_MODEL_SCORE_TYPE = "PWM score";
    public static final String PVALUE_SCORE_TYPE = "-log10(PWM p-value)";
    private String scoreType = SITE_MODEL_SCORE_TYPE;
    @PropertyName("Score type")
    @PropertyDescription("The type of position weight matrix score")
    public String getScoreType()
    {
        return scoreType;
    }
    public void setScoreType(String scoreType)
    {
        String oldValue = this.scoreType;
        this.scoreType = scoreType;
        firePropertyChange( "scoreType", oldValue, scoreType );
    }
    public boolean isScoreTypeHidden()
    {
        if(siteModels == null)
            return true;
        SiteModelCollection smc = siteModels.optDataElement( SiteModelCollection.class );
        if(smc == null || smc.isEmpty())
            return true;
        SiteModel sm = smc.iterator().next();
        if(sm == null || sm.getPValueCutoff() == null)
            return true;
        return false;
    }
    public boolean isPValueMode()
    {
        return scoreType.contentEquals( PVALUE_SCORE_TYPE );
    }

    @PropertyName("Score difference")
    @PropertyDescription("Minimal site score difference between reference and alternative sequences")
    public double getScoreDiff()
    {
        return scoreDiff;
    }
    public void setScoreDiff(double scoreDiff)
    {
        Object oldValue = this.scoreDiff;
        this.scoreDiff = scoreDiff;
        firePropertyChange( "scoreDiff", oldValue, scoreDiff );
    }

    private boolean independentVariations = false;
    @PropertyName("Independent variations")
    @PropertyDescription("Treat each variation independently")
    public boolean getIndependentVariations()
    {
        return independentVariations;
    }
    public void setIndependentVariations(boolean value)
    {
        boolean oldValue = this.independentVariations;
        this.independentVariations = value;
        firePropertyChange( "independetVariations", oldValue, independentVariations );
    }
    
    private int targetGeneTSSUpstream = 5000;
    @PropertyName("Target gene TSS upstream")
    @PropertyDescription("When searching for target genes, match TF binding sites to this distance upstream of TSS.")
    public int getTargetGeneTSSUpstream()
    {
        return targetGeneTSSUpstream;
    }
    public void setTargetGeneTSSUpstream(int targetGeneTSSUpstream)
    {
        int oldValue = this.targetGeneTSSUpstream;
        this.targetGeneTSSUpstream = targetGeneTSSUpstream;
        firePropertyChange( "targetGeneTSSUpstream", oldValue, targetGeneTSSUpstream );
    }

    private int targetGeneTSSDownstream = 500;
    @PropertyName("Target gene TSS downstream")
    @PropertyDescription("When searching for target genes, match TF binding sites to this distance downstream of TSS.")
    public int getTargetGeneTSSDownstream()
    {
        return targetGeneTSSDownstream;
    }
    public void setTargetGeneTSSDownstream(int targetGeneTSSDownstream)
    {
        int oldValue = this.targetGeneTSSDownstream;
        this.targetGeneTSSDownstream = targetGeneTSSDownstream;
        firePropertyChange( "targetGeneTSSDownstream", oldValue, targetGeneTSSDownstream );
    }
    
    private boolean oneNearestTargetGene = false;
    @PropertyName("One nearest target gene")
    @PropertyDescription("Report only one target gene nearest to TSS.")
    public boolean isOneNearestTargetGene()
    {
        return oneNearestTargetGene;
    }
    public void setOneNearestTargetGene(boolean oneNearestTargetGene)
    {
        boolean oldValue = this.oneNearestTargetGene;
        this.oneNearestTargetGene = oneNearestTargetGene;
        firePropertyChange( "oneNearestTargetGene", oldValue, oneNearestTargetGene );
    }

    private DataElementPath outputTable;
    @PropertyName("Output table")
    @PropertyDescription("Output table")
    public DataElementPath getOutputTable()
    {
        return outputTable;
    }

    public void setOutputTable(DataElementPath outputTable)
    {
        Object oldValue = this.outputTable;
        this.outputTable = outputTable;
        firePropertyChange( "outputTable", oldValue, outputTable );
    }

    @PropertyName("Site gain track")
    @PropertyDescription("Resulting track with site gain events")
    public DataElementPath getSiteGainTrack()
    {
        return siteGainTrack;
    }
    public void setSiteGainTrack(DataElementPath siteGainTrack)
    {
        Object oldValue = this.siteGainTrack;
        this.siteGainTrack = siteGainTrack;
        firePropertyChange( "siteGainTrack", oldValue, siteGainTrack );
    }

    @PropertyName("Site loss track")
    @PropertyDescription("Resulting track with site gain events")
    public DataElementPath getSiteLossTrack()
    {
        return siteLossTrack;
    }
    public void setSiteLossTrack(DataElementPath siteLossTrack)
    {
        Object oldValue = this.siteLossTrack;
        this.siteLossTrack = siteLossTrack;
        firePropertyChange( "siteLossTrack", oldValue, siteLossTrack );
    }
    
    private DataElementPath importantMutationsTrack;
    @PropertyName("Important mutations")
    @PropertyDescription("Track of mutations that affects binding sites")
    public DataElementPath getImportantMutationsTrack()
    {
        return importantMutationsTrack;
    }
    public void setImportantMutationsTrack(DataElementPath importantMutationsTrack)
    {
        Object oldValue = this.importantMutationsTrack;
        this.importantMutationsTrack = importantMutationsTrack;
        firePropertyChange( "importantMutationsTrack", oldValue, importantMutationsTrack );
    }

    @PropertyName("Summary table")
    @PropertyDescription("Summary table giving site model frequencies per mutation")
    public DataElementPath getSummaryTable()
    {
        return summaryTable;
    }
    public void setSummaryTable(DataElementPath summaryTable)
    {
        Object oldValue = this.summaryTable;
        this.summaryTable = summaryTable;
        firePropertyChange( "summaryTable", oldValue, summaryTable );
    }
}
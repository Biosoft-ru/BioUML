package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.ComplexAnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.access.SequencesDatabaseInfo;

@ClassIcon ( "resources/sitesresult.gif" )
public class GeneSetSiteSearch extends ComplexAnalysisMethodSupport<GeneSetSiteSearchParameters>
{
    private static final String NO_SITES_JOB = "Searching no sites";
    private static final String NO_TRACKS_JOB = "Extracting no promoters";
    private static final String SUMMARY_JOB = "Generating summary";
    private static final String OPTIMIZE_JOB = "Optimizing cutoffs and generating summary";
    private static final String YES_SITES_JOB = "Searching yes sites";
    private static final String SITES_JOB = "Searching sites";
    private static final String YES_TRACKS_JOB = "Extracting yes promoters";
    private static final String TRACKS_JOB = "Extracting promoters";
    private GeneSetToTrack yesTracks;
    private GeneSetToTrack noTracks;
    private SiteSearchAnalysis yesSites;
    private SiteSearchAnalysis noSites;
    private AnalysisMethod summary;

    public GeneSetSiteSearch(DataCollection<?> origin, String name)
    {
        super(origin, name, JavaScriptBSA.class, new GeneSetSiteSearchParameters());
    }

    protected void initYesPromoterExtractAnalysis()
    {
        GeneSetToTrackParameters parameters = yesTracks.getParameters();
        parameters.setSourcePath(this.parameters.getYesSetPath());
        parameters.setFrom(this.parameters.getFrom());
        parameters.setTo(this.parameters.getTo());
        parameters.setSpecies(this.parameters.getSpecies());
        parameters.setDestPath(this.parameters.getOutputPath().getChildPath(SiteSearchResult.YES_PROMOTERS));
    }

    protected void initNoPromoterExtractAnalysis()
    {
        GeneSetToTrackParameters parameters = noTracks.getParameters();
        parameters.setSourcePath(this.parameters.getNoSetPath());
        parameters.setFrom(this.parameters.getFrom());
        parameters.setTo(this.parameters.getTo());
        parameters.setSpecies(this.parameters.getSpecies());
        parameters.setDestPath(this.parameters.getOutputPath().getChildPath(SiteSearchResult.NO_PROMOTERS));
    }
    
    protected void initYesSiteSearchAnalysis() throws Exception
    {
        SiteSearchAnalysisParameters parameters = yesSites.getParameters();
        parameters.setProfile(this.parameters.getProfile());
        Track track = this.parameters.getOutputPath().getChildPath(SiteSearchResult.YES_PROMOTERS).getDataElement(Track.class);
        parameters.setDbSelector( SequencesDatabaseInfo.CUSTOM_SEQUENCES );
        parameters.setSeqCollectionPath( TrackUtils.getTrackSequencesPath( track ) );
        log.info("Yes species: " + this.parameters.getSpecies().toString());
        log.info("Yes Ensembl: " + TrackUtils.getEnsemblPath(this.parameters.getSpecies()).toString());
        log.info("Yes seqCollectionPath: " + parameters.getSeqCollectionPath().toString());
        parameters.setTrack(track);
        parameters.setOutput(this.parameters.getOutputPath().getChildPath(SiteSearchResult.YES_SITES));
    }

    protected void initNoSiteSearchAnalysis() throws Exception
    {
        SiteSearchAnalysisParameters parameters = noSites.getParameters();
        parameters.setProfile(this.parameters.getProfile());
        Track track = this.parameters.getOutputPath().getChildPath(SiteSearchResult.NO_PROMOTERS).getDataElement(Track.class);
        parameters.setDbSelector( SequencesDatabaseInfo.CUSTOM_SEQUENCES );
        parameters.setSeqCollectionPath( TrackUtils.getTrackSequencesPath( track ) );
        log.info("No species: " + this.parameters.getSpecies().toString());
        log.info("No Ensembl: " + TrackUtils.getEnsemblPath(this.parameters.getSpecies()).toString());
        log.info("No seqCollectionPath: " + parameters.getSeqCollectionPath().toString());
        parameters.setTrack(track);
        parameters.setOutput(this.parameters.getOutputPath().getChildPath(SiteSearchResult.NO_SITES));
    }
    
    protected void initSummaryAnalysis() throws Exception
    {
        SiteSearchSummaryParameters parameters = (SiteSearchSummaryParameters)summary.getParameters();
        parameters.setYesTrackPath(this.parameters.getOutputPath().getChildPath(SiteSearchResult.YES_SITES));
        parameters.setNoTrackPath(this.parameters.getNoSetPath() == null ? null : this.parameters.getOutputPath().getChildPath(SiteSearchResult.NO_SITES));
        parameters.setOverrepresentedOnly(this.parameters.isOverrepresentedOnly());
        parameters.setOutputPath(this.parameters.getOutputPath().getChildPath(SiteSearchResult.SUMMARY));
    }
    
    protected void initOptimizationAnalysis()
    {
        OptimizeSiteSearchAnalysisParameters parameters = (OptimizeSiteSearchAnalysisParameters)summary.getParameters();
        parameters.setInYesTrack(this.parameters.getOutputPath().getChildPath(SiteSearchResult.YES_SITES));
        parameters.setInNoTrack(this.parameters.getNoSetPath() == null ? null : this.parameters.getOutputPath().getChildPath(SiteSearchResult.NO_SITES));
        parameters.setOutYesTrack(this.parameters.getOutputPath().getChildPath(SiteSearchResult.YES_SITES_OPTIMIZED));
        parameters.setOutNoTrack(this.parameters.getNoSetPath() == null ? null : this.parameters.getOutputPath().getChildPath(SiteSearchResult.NO_SITES_OPTIMIZED));
        parameters.setOutSummaryTable(this.parameters.getOutputPath().getChildPath(SiteSearchResult.SUMMARY));
        parameters.setPvalueCutoff(this.parameters.getPvalueCutoff());
        parameters.setOverrepresentedOnly(this.parameters.isOverrepresentedOnly());
        if(this.parameters.getOptimizeCutoff() && this.parameters.getOptimizeWindow())
            parameters.setOptimizationType(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_BOTH);
        else if(this.parameters.getOptimizeWindow())
            parameters.setOptimizationType(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_WINDOW);
        else
            parameters.setOptimizationType(OptimizeSiteSearchAnalysisParameters.OPTIMIZE_CUTOFF);
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths(new String[] {"yesSetPath", "noSetPath", "profilePath"}, parameters.getOutputNames());
        checkNotEmptyCollection("yesSetPath");
        checkNotEmptyCollection("profilePath");
        if(this.parameters.getOptimizeCutoff() && this.parameters.getNoSet() == null)
            throw new IllegalArgumentException("For cutoff optimization you need to specify no-set. Either specify no-set or uncheck the 'optimize cutoff' option");
    }
    
    private void initSubJobs()
    {
        validateParameters();
        yesTracks = AnalysisMethodRegistry.getAnalysisMethod( GeneSetToTrack.class );
        yesTracks.setLogger(getLogger());
        yesSites = AnalysisMethodRegistry.getAnalysisMethod( SiteSearchAnalysis.class );
        yesSites.setLogger(getLogger());
        summary = this.parameters.getOptimizeCutoff() || this.parameters.getOptimizeWindow()
                ? AnalysisMethodRegistry.getAnalysisMethod( OptimizeSiteSearchAnalysis.class )
                : AnalysisMethodRegistry.getAnalysisMethod( SiteSearchSummary.class );
        summary.setLogger(getLogger());
        if(parameters.getNoSet() == null)
        {
            addAnalysis(yesTracks, 20, TRACKS_JOB);
            addAnalysis(yesSites, 90, SITES_JOB);
            addAnalysis(summary, 100, SUMMARY_JOB);
        } else
        {
            noTracks = AnalysisMethodRegistry.getAnalysisMethod( GeneSetToTrack.class );
            noTracks.setLogger(getLogger());
            noSites = AnalysisMethodRegistry.getAnalysisMethod( SiteSearchAnalysis.class );
            noSites.setLogger(getLogger());
            if(this.parameters.getOptimizeCutoff() || this.parameters.getOptimizeWindow())
            {
                addAnalysis(yesTracks, 5, YES_TRACKS_JOB);
                addAnalysis(noTracks, 10, NO_TRACKS_JOB);
                addAnalysis(yesSites, 35, YES_SITES_JOB);
                addAnalysis(noSites, 60, NO_SITES_JOB);
                addAnalysis(summary, 100, OPTIMIZE_JOB);
            } else
            {
                addAnalysis(yesTracks, 10, YES_TRACKS_JOB);
                addAnalysis(noTracks, 20, NO_TRACKS_JOB);
                addAnalysis(yesSites, 55, YES_SITES_JOB);
                addAnalysis(noSites, 90, NO_SITES_JOB);
                addAnalysis(summary, 100, SUMMARY_JOB);
            }
        }
    }
    
    public void initResultItem() throws Exception
    {
        validateParameters();
        DataCollection<?> resultItem = SiteSearchResult.createResult( parameters.getOutputPath() );
        resultItem.getInfo().getProperties().setProperty( TrackUtils.ENSEMBL_PATH_PROPERTY,
                TrackUtils.getEnsemblPath( parameters.getSpecies(), parameters.getOutputPath() ).toString() );
        this.writeProperties(resultItem);
        parameters.getOutputPath().save(resultItem);
    }

    @Override
    public void beforeRun() throws Exception
    {
        initResultItem();
        initSubJobs();
    }
    
    @Override
    public void beforeJob(String name) throws Exception
    {
        if(name.equals(TRACKS_JOB) || name.equals(YES_TRACKS_JOB))
            initYesPromoterExtractAnalysis();
        else if(name.equals(NO_TRACKS_JOB))
            initNoPromoterExtractAnalysis();
        else if(name.equals(SITES_JOB) || name.equals(YES_SITES_JOB))
            initYesSiteSearchAnalysis();
        else if(name.equals(NO_SITES_JOB))
            initNoSiteSearchAnalysis();
        else if(name.equals(SUMMARY_JOB))
            initSummaryAnalysis();
        else if(name.equals(OPTIMIZE_JOB))
            initOptimizationAnalysis();
    }

    @Override
    protected void afterRun() throws Exception
    {
        if((this.parameters.getOptimizeCutoff() || this.parameters.getOptimizeWindow())
                && this.parameters.isDeleteNonOptimized())
        {
            this.parameters.getOutputPath().getChildPath(SiteSearchResult.YES_SITES).remove();
            this.parameters.getOutputPath().getChildPath(SiteSearchResult.NO_SITES).remove();
        }
    }
}

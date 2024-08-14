package ru.biosoft.bsa.analysis;

import org.mozilla.javascript.Scriptable;

import biouml.standard.type.Species;

import ru.biosoft.jobcontrol.JobControl;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.access.SequencesDatabaseInfo;
import ru.biosoft.bsa.analysis.createsitemodel.CreateIPSModel;
import ru.biosoft.bsa.analysis.createsitemodel.CreateIPSModelParameters;
import ru.biosoft.bsa.analysis.createsitemodel.CreateMatchModel;
import ru.biosoft.bsa.analysis.createsitemodel.CreateMatchModelParameters;
import ru.biosoft.bsa.analysis.createsitemodel.CreateWeightMatrixModel;
import ru.biosoft.bsa.analysis.createsitemodel.CreateWeightMatrixModelParameters;
import ru.biosoft.bsa.analysis.motifcompare.MotifCompare;
import ru.biosoft.bsa.analysis.motifcompare.MotifCompareParameters;
import ru.biosoft.bsa.macs.MACSAnalysis;
import ru.biosoft.bsa.macs.MACSAnalysisParameters;
import ru.biosoft.bsa.macs14.MACS14Analysis;
import ru.biosoft.bsa.macs14.MACS14AnalysisParameters;
import ru.biosoft.bsa.project.Project;
import ru.biosoft.bsa.project.ProjectAsLists;
import ru.biosoft.bsa.project.Region;
import ru.biosoft.bsa.project.TrackInfo;
import ru.biosoft.plugins.javascript.JSAnalysis;
import ru.biosoft.plugins.javascript.JSDescription;
import ru.biosoft.plugins.javascript.JSProperty;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.table.TableDataCollection;
 
public class JavaScriptBSA extends JavaScriptHostObjectBase
{
    public JavaScriptBSA()
    {
    }

    /**
     * Perform site search analysis
     * @param sequences - Collection containing input sequences
     * @param profile - profile collection (should contain WeightMatrix objects)
     * @param intervals - if not null then used to filter input
     * @param outputPath - name of output Track with complete path
     * @return Track - created track
     * @throws Exception in various cases
     */
    public Track siteSearch(DataCollection<?> sequences, DataCollection<?> profile, Track intervals, String outputPath) throws Throwable
    {
        SiteSearchAnalysis analysis = new SiteSearchAnalysis(null, "");
        SiteSearchAnalysisParameters parameters = analysis.getParameters();
        parameters.setDbSelector( SequencesDatabaseInfo.CUSTOM_SEQUENCES );
        parameters.setSeqCollection(sequences);
        parameters.setProfile(profile);
        parameters.setTrack(intervals);
        parameters.setOutput(outputPath==null?null:DataElementPath.create(outputPath));
        return analysis.justAnalyzeAndPut();
    }
    
    /**
     * Generate summary table on site search analysis
     * @param yesTrack - Site search analysis resulting track for experiment data
     * @param noTrack - Site search analysis resulting track for background data (can be null)
     * @param overrepresentedOnly - Remove underrepresented matrices from the output (not applicable if no-set is not specified)
     * @param outputPath - name of output TableDataCollection with complete path
     * @return TableDataCollection - created summary table
     * @throws Exception in various cases
     */
    @JSAnalysis(SiteSearchSummary.class)
    public TableDataCollection siteSearchSummary(
            @JSProperty("yesTrackPath") Track yesTrack,
            @JSProperty("noTrackPath") Track noTrack,
            @JSProperty("overrepresentedOnly") boolean overrepresentedOnly,
            @JSProperty("outputPath") String outputPath) throws Exception
    {
        SiteSearchSummary analysis = new SiteSearchSummary(null, "");
        SiteSearchSummaryParameters parameters = analysis.getParameters();
        parameters.setYesTrack(yesTrack);
        parameters.setNoTrack(noTrack);
        parameters.setOverrepresentedOnly(overrepresentedOnly);
        parameters.setOutputPath(DataElementPath.create(outputPath));
        analysis.validateParameters();
        return analysis.justAnalyzeAndPut();
    }

    /**
     * Search sites on gene set
     * @param yesSet - set of genes expressed in experiment
     * @param noSet - set of background genes (can be null)
     * @param species - Species latin name used during matching
     * @param from - promoter window from value (relative to gene start)
     * @param to - promoter window to value (relative to gene start)
     * @param profile - profile collection (should contain WeightMatrix objects)
     * @param outputPath - output path and name (must be inside GenericDataCollection)
     * @param optimizeCutoff - whether to optimize cutoffs
     * @param optimizeWindow - whether to optimize from/to positions
     * @param pvalueCutoff - exclude sites of matrices for which cutoff is higher than specified value (ignored if no optimization is performed)
     * @param overrepresentedOnly - Remove underrepresented matrices from the output (not applicable if no-set is not specified)
     * @throws Exception in various cases
     */
    @JSAnalysis(GeneSetSiteSearch.class)
    public void geneSetSiteSearch(
            @JSProperty("yesSetPath") DataCollection<?> yesSet,
            @JSProperty("noSetPath") @JSDescription("Set of background genes (can be null)") DataCollection<?> noSet,
            @JSProperty("species") @JSDescription("Species latin name used during matching (E.g. 'Homo sapiens')") String species,
            @JSProperty("from") int from,
            @JSProperty("to") int to,
            @JSProperty("profilePath") DataCollection<?> profile,
            @JSProperty("outputPath") String outputPath,
            @JSProperty("optimizeCutoff") boolean optimizeCutoff,
            @JSProperty("optimizeWindow") boolean optimizeWindow,
            @JSProperty("pvalueCutoff") double pvalueCutoff,
            @JSProperty("overrepresentedOnly") boolean overrepresentedOnly
            )
    {
        GeneSetSiteSearch analysis = new GeneSetSiteSearch(null, "");
        GeneSetSiteSearchParameters parameters = analysis.getParameters();
        parameters.setYesSet(yesSet);
        parameters.setNoSet(noSet);
        parameters.setSpecies(Species.getSpecies(species));
        parameters.setFrom(from);
        parameters.setTo(to);
        parameters.setProfile(profile);
        parameters.setOutputPath(DataElementPath.create(outputPath));
        parameters.setOptimizeCutoff(optimizeCutoff);
        parameters.setOptimizeWindow(optimizeWindow);
        parameters.setPvalueCutoff(pvalueCutoff);
        JobControl job = analysis.getJobControl();
        job.run();
    }
    
    
    @JSAnalysis(ProcessTrack.class)
    public void processTrack(
            @JSProperty("sourcePath") Track source,
            @JSProperty("sequences") String sequences,
            @JSProperty("enlargeStart") int enlargeStart,
            @JSProperty("enlargeEnd") int enlargeEnd,
            @JSProperty("mergeOverlapping") boolean mergeOverlapping,
            @JSProperty("minimalSize") int minimalSize,
            @JSProperty("destPath") String outputPath
            )
    {
        ProcessTrack analysis = new ProcessTrack(null, "");
        ProcessTrackParameters parameters = analysis.getParameters();
        parameters.setSourcePath(DataElementPath.create(source));
        parameters.setSequences(DataElementPath.create( sequences ));
        parameters.setEnlargeStart(enlargeStart);
        parameters.setEnlargeEnd(enlargeEnd);
        parameters.setMergeOverlapping(mergeOverlapping);
        parameters.setMinimalSize(minimalSize);
        parameters.setDestPath(DataElementPath.create(outputPath));
        JobControl job = analysis.getJobControl();
        job.run();
    }
    
    /**
     * Perform an optimization of site search analysis results
     * @param inYesTrack Site search result for genes expressed in experiment
     * @param inNoTrack Site search result for background genes
     * @param optimizationType Whether to optimize only matrices cutoffs, promoter windows or both. Valid values are "Cutoffs", "Window" or "Both"
     * @param pvalueCutoff Matrix will be removed from result if p-value cannot be optimized to values lower than specified cutoff
     * @param outYesTrack Path to filtered site search result for expressed genes (collection must accept SqlTrack; can be null if you don't want this)
     * @param outNoTrack Path to filtered site search result for background genes (collection must accept SqlTrack; can be null if you don't want this)
     * @param overrepresentedOnly - Remove underrepresented matrices from the output (not applicable if no-set is not specified)
     * @param outSummary Path to summary table (collection must accept TableDataCollection)
     * @throws Exception in various cases
     */
    @JSAnalysis(OptimizeSiteSearchAnalysis.class)
    public void optimizeSiteSearchAnalysis(
            @JSProperty("inYesTrack") Track inYesTrack,
            @JSProperty("inNoTrack") Track inNoTrack,
            @JSProperty("optimizationType") String optimizationType,
            @JSProperty("pvalueCutoff") double pvalueCutoff,
            @JSProperty("outYesTrack") String outYesTrack,
            @JSProperty("outNoTrack") String outNoTrack,
            @JSProperty("overrepresentedOnly") boolean overrepresentedOnly,
            @JSProperty("outSummaryTable") String outSummary)
    {
        OptimizeSiteSearchAnalysis analysis = new OptimizeSiteSearchAnalysis(null, "");
        OptimizeSiteSearchAnalysisParameters parameters = analysis.getParameters();
        parameters.setInYesTrack(DataElementPath.create(inYesTrack));
        parameters.setInNoTrack(DataElementPath.create(inNoTrack));
        parameters.setOptimizationType(optimizationType);
        parameters.setPvalueCutoff(pvalueCutoff);
        parameters.setOutYesTrack(DataElementPath.create(outYesTrack));
        parameters.setOutNoTrack(DataElementPath.create(outNoTrack));
        parameters.setOutSummaryTable(DataElementPath.create(outSummary));
        parameters.setOverrepresentedOnly(overrepresentedOnly);
        JobControl job = analysis.getJobControl();
        job.run();
    }
    
    /**
    * Model-based analysis of ChiP-Seq data
    * @param track - name of CHiP-Seq Track to analyze
    * @param controlTrack - name of Track to be used as control
    * @param outputPath - name of output Track with complete path
    * @param fixedLambda - If true, uses fixed lambda value. If false, uses three levels of nearby region in basepairs to calculate dynamic lambda.
    * @param lambda1 - 1st level of nearby region to calculate dynamic lambda (in basepairs)
    * @param lambda2 - 2nd level of nearby region to calculate dynamic lambda (in basepairs)
    * @param lambda3 - 3rd level of nearby region to calculate dynamic lambda (in basepairs)
    * @param noModel - if true, MACS will not build model
    * @param shiftSize - the arbitrary shift size in bp, is used when noModel is true
    * @param bandWidth - band width, if noModel is true, 2 time of this value will be used as a scanwindow width
    * @param genomeSize - effective genome size
    * @param enrichmentRatio - enrichment ratio against background of regions to be taken to build model
    * @param tagSize - tag size
    * @param pValue - Pvalue cutoff for peak detection
    * @param FDR - if true, False Discovery Rate is calculated
    * @return Track - Created track
     * @throws Exception
    */
    
    public Track MACS(Track track, Track controlTrack, String outputPath, boolean fixedLambda, int lambda1, int lambda2, int lambda3,
            boolean noModel, int shiftSize, int bandWidth, double genomeSize, int enrichmentRatio, int tagSize, double pValue, boolean FDR) throws Exception
    {
        MACSAnalysis analysis = new MACSAnalysis(null, "");
        MACSAnalysisParameters parameters = analysis.getParameters();
        parameters.setTrack(track);
        parameters.setControlTrack(controlTrack);
        parameters.setOutputPath(DataElementPath.create(outputPath));
        parameters.setNolambda(fixedLambda);
        int[] lambdaSet = new int[3];
        lambdaSet[0] = lambda1;
        lambdaSet[1] = lambda2;
        lambdaSet[2] = lambda3;
        parameters.setLambdaSetArray(lambdaSet);
        parameters.setNomodel(noModel);
        parameters.setShiftsize(shiftSize);
        parameters.setBw(bandWidth);
        parameters.setGsize(genomeSize);
        parameters.setMfold(enrichmentRatio);
        parameters.setTsize(tagSize);
        parameters.setPvalue(pValue);
        parameters.setFutureFDR(FDR);
        return analysis.justAnalyzeAndPut();
    }
    
    @JSAnalysis(MACS14Analysis.class)
    public Track MACS14(
            @JSProperty("trackPath") Track track,
            @JSProperty("controlPath") Track controlTrack,
            @JSProperty("outputPath") String outputPath,
            Scriptable params) throws Exception
    {
        MACS14Analysis analysis = new MACS14Analysis(null, "");
        MACS14AnalysisParameters parameters = analysis.getParameters();
        parameters.setTrack(track);
        parameters.setControlTrack(controlTrack);
        parameters.setOutputPath(DataElementPath.create(outputPath));
        parameters.setFromScriptable(params);
        return analysis.justAnalyzeAndPut();
    }
    
    @JSAnalysis(CreateMatchModel.class)
    public SiteModel createMatchModel(
            @JSProperty("frequencyMatrix") FrequencyMatrix matrix,
            @JSProperty("outputPath") String outputPath,
            Scriptable params ) throws Exception
    {
        CreateMatchModel analysis = new CreateMatchModel(null, "");
        CreateMatchModelParameters parameters = analysis.getParameters();
        parameters.setMatrixPath(DataElementPath.create(matrix));
        parameters.setModelPath(DataElementPath.create(outputPath));
        parameters.setFromScriptable(params);
        return analysis.justAnalyzeAndPut();
    }
    
    @JSAnalysis(CreateWeightMatrixModel.class)
    public SiteModel createWeightMatrixModel(
            @JSProperty("frequencyMatrix") FrequencyMatrix matrix,
            @JSProperty("outputPath") String outputPath,
            Scriptable params ) throws Exception
    {
        CreateWeightMatrixModel analysis = new CreateWeightMatrixModel(null, "");
        CreateWeightMatrixModelParameters parameters = analysis.getParameters();
        parameters.setMatrixPath(DataElementPath.create(matrix));
        parameters.setModelPath(DataElementPath.create(outputPath));
        parameters.setFromScriptable(params);
        return analysis.justAnalyzeAndPut();
    }
    
    @JSAnalysis(CreateIPSModel.class)
    public SiteModel createIPSModel(
            @JSProperty("frequencyMatrix") FrequencyMatrix[] matrices,
            @JSProperty("outputPath") String outputPath,
            Scriptable params ) throws Exception
    {
        CreateIPSModel analysis = new CreateIPSModel(null, "");
        CreateIPSModelParameters parameters = analysis.getParameters();
        
        DataElementPathSet matricesPathSet = new DataElementPathSet();
        for(FrequencyMatrix m : matrices)
            matricesPathSet.add(DataElementPath.create(m));
        parameters.setFrequencyMatrices(matricesPathSet);
        
        parameters.setModelPath(DataElementPath.create(outputPath));
        parameters.setFromScriptable(params);
        return analysis.justAnalyzeAndPut();
    }
    
    public Project createProject(AnnotatedSequence sequenceMap, Track[] tracks)
    {
        Project result = new ProjectAsLists(sequenceMap.getName(), null);
        result.addRegion(new Region(sequenceMap));
        for(Track track: tracks)
        {
            result.addTrack(new TrackInfo(track));
        }
        return result;
    }
    
    @JSAnalysis(FilterTrackAnalysis.class)
    public Track filterOneTrackByAnother(
            @JSProperty("inputTrack") Track inputTrack,
            @JSProperty("filterTrack") Track filterTrack,
            @JSProperty("outputPath") String outputPath,
            Scriptable params) throws Exception
    {
        FilterTrackAnalysis analysis = new FilterTrackAnalysis( null, "" );
        FilterTrackAnalysisParameters parameters = analysis.getParameters();
        parameters.setInputTrack( DataElementPath.create( inputTrack ) );
        parameters.setFilterTrack( DataElementPath.create( filterTrack ) );
        parameters.setOutputTrack( DataElementPath.create( outputPath ) );
        parameters.setFromScriptable( params );
        return analysis.justAnalyzeAndPut();
    }
    
    @JSAnalysis ( MotifCompare.class )
    public TableDataCollection compareMotifs(
            @JSProperty ( "sequences" ) DataCollection<AnnotatedSequence> sequences,
            @JSProperty ( "siteModels" ) DataElementPathSet siteModels,
            @JSProperty ( "output" ) String output,
            Scriptable params) throws Exception
    {
        MotifCompare analysis = new MotifCompare(null, "");
        MotifCompareParameters parameters = analysis.getParameters();
        parameters.setSequences(DataElementPath.create(sequences));
        parameters.setSiteModels(siteModels);
        parameters.setFromScriptable( params );
        parameters.setOutput(DataElementPath.create(output));
        return analysis.justAnalyzeAndPut();
    }
}

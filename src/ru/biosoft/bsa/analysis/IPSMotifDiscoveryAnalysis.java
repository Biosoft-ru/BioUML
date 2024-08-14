package ru.biosoft.bsa.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.transformer.WeightMatrixTransformer;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.journal.ProjectUtils;

@ClassIcon( "resources/IPS-motif-discovery.gif" )
public class IPSMotifDiscoveryAnalysis extends AnalysisMethodSupport<IPSMotifDiscoveryAnalysisParameters>
{
    private static final double MAX_THRESHOLD = 0.85;

    public IPSMotifDiscoveryAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new IPSMotifDiscoveryAnalysisParameters());
    }

    private List<Sequence> sequences = null;

    private List<Sequence> getSequences() throws Exception
    {
        if(sequences != null)
            return sequences;
        List<Sequence> result = new ArrayList<>();

        DataElementPath sourceSequences = parameters.getSequencesPath();

        DataElementPath processedSequences = sourceSequences;

        if( sourceSequences.optDataElement() instanceof SqlTrack )
        {
            ProcessTrack processTrackAnalysis = new ProcessTrack(null, "Process track for IPS motif discovery");

            ProcessTrackParameters processTrackParameters = processTrackAnalysis.getParameters();
            processTrackParameters.setEnlargeStart(parameters.getWindowSize() / 2);
            processTrackParameters.setEnlargeEnd(parameters.getWindowSize() / 2);
            processTrackParameters.setMergeOverlapping(true);
            processTrackParameters.setSourcePath(sourceSequences);

            SqlTrack track = sourceSequences.getDataElement(SqlTrack.class);
            processTrackParameters.setSequences(track.getChromosomesPath());

            DataElementPath projectPath = ProjectUtils.getProjectPath( parameters.getOutputPath() );
            processedSequences = projectPath.getChildPath("tmp", UUID.randomUUID().toString());
            processTrackParameters.setDestPath(processedSequences);

            processTrackAnalysis.setLogger(getLogger());
            processTrackAnalysis.getJobControl().run();
        }

        for(AnnotatedSequence as : processedSequences.getDataCollection(AnnotatedSequence.class))
        {
            result.add( as.getSequence());
        }
        if( sourceSequences.optDataElement() instanceof SqlTrack )
          processedSequences.remove();
        
        sequences = result;
        return result;
    }

    private IPSSiteModel constructIPSSiteModel() throws Exception
    {
        if( parameters.getInitialMatrices().isEmpty() )
            throw new Exception( "Please provide at least one initial position weight matrix" );
        DataElementPath outputPath = parameters.getOutputPath();
        if( !outputPath.exists() )
        {
            log.info("No matrix library found, creating");
            WeightMatrixTransformer.createMatrixLibrary(outputPath);
        }
        DataCollection<?> outputMatrixLib = outputPath.optDataCollection();

        WeightMatrixModel[] matrices = parameters.getInitialMatrices().elements( FrequencyMatrix.class )
                .map( pfm -> new FrequencyMatrix( outputMatrixLib, pfm.getName() + "_IPS", pfm ) )
                .map( pfm -> new WeightMatrixModel( "(none)", null, pfm, 0 ) ).peek( wmm -> wmm.setThreshold( wmm.getMaxScore() / 2 ) )
                .toArray( WeightMatrixModel[]::new );
        return new IPSSiteModel( "(none)", null, matrices, parameters.getCritIPS(), 0, parameters.getWindowSize() );
    }
    
    @Override
    public IPSSiteModel justAnalyzeAndPut() throws Exception
    {
        jobControl.setPreparedness(0);
        log.info("IPS Motif Discovery Analysis started");

        IPSSiteModel model = constructIPSSiteModel();

        if(parameters.isExtendInitialMatrices())
            model = optimizeSplittedMatrices(getSequences(), model, parameters.getMaxIterations(), parameters.getMinClusterSize(), jobControl);
        else
            optimizeIPSModel(getSequences(), model, parameters.getMaxIterations(), parameters.getMinClusterSize(), jobControl);
        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
        {
            log.info("IPS Motif Discovery terminated by request");
            return null;
        }

        //save matrices
        for( int i = 0; i < model.getMatrices().length; i++ )
        {
            FrequencyMatrix matrix = model.getMatrices()[i].getFrequencyMatrix();
            CollectionFactoryUtils.save(matrix);
        }

        jobControl.setPreparedness(100);
        log.info("IPS Motif Discovery finished");

        return model;
    }

    public IPSSiteModel optimizeSplittedMatrices(List<Sequence> sequences, IPSSiteModel model, int maxIterations, int minClusterSize,
            AnalysisJobControl jobControl) throws Exception
    {
        IPSSiteModel[] models = splitMatrixBasedTwoMaximalFrequencies(model);
        if(models.length == 0)
        {
            log.warning("There no positions esential for splitting");
            return null;
        }
        log.info("Matrix was splitted on " + models.length + " positions");
        for(int i = 0; i < models.length; i++)
        {
            log.info("Optimizing "+ i + " model");
            jobControl.pushProgress( i * 100 / models.length, ( i + 1 ) * 100 / models.length );
            optimizeIPSModel(sequences, models[i], maxIterations, minClusterSize, jobControl);
        }
        
        IPSSiteModel bestModel = null;
        double bestL1Norm = 0;
        for(IPSSiteModel m : models)
        {
            WeightMatrixModel[] weightMatrices = m.getMatrices();
            if(weightMatrices.length != 2)
                continue;
            double L1Norm = FrequencyMatrix.L1NormDiff(weightMatrices[0].getFrequencyMatrix(), weightMatrices[1].getFrequencyMatrix());
            if(L1Norm > bestL1Norm)
            {
                bestL1Norm = L1Norm;
                bestModel = m;
            }
        }
        return bestModel;
    }
    
    public static void optimizeIPSModel(List<Sequence> sequences, IPSSiteModel model, int maxIterations, int minClusterSize,
            JobControl jobControl) throws Exception
    {
        Site[] prevAlignment = null;
        for( int iter = 0; iter < maxIterations; iter++ )
        {
            if( jobControl != null )
            {
                jobControl.setPreparedness(iter * 100 / maxIterations);
                if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
                    return;
            }
    
            Site[] alignment = searchBestSites(model, sequences);
            if( alignmentsEqual(prevAlignment, alignment) )
                break;
            updateIPSModel(model, alignment, minClusterSize);
            prevAlignment = alignment;
        }
    }

    public static FrequencyMatrix getMatrixFromIpsBestSites(Sequence[] sequences, FrequencyMatrix frequencyMatrix, DataCollection<?> dataCollectionForNewMatrix, String newMatrixName) throws Exception
    {
        FrequencyMatrix matrix = new FrequencyMatrix(dataCollectionForNewMatrix, newMatrixName, frequencyMatrix);
        WeightMatrixModel weightMatrixModel = new WeightMatrixModel(matrix.getName(), null, matrix, 0.0);
        IPSSiteModel ipsSiteModel = new IPSSiteModel(weightMatrixModel.getName(), null, new WeightMatrixModel[] {weightMatrixModel}, 0.0, IPSSiteModel.DEFAULT_DIST_MIN, matrix.getLength());
        List<Sequence> seqsForAlignment = new ArrayList<>();
        for( Sequence sequence : sequences )
            seqsForAlignment.add(sequence);
        optimizeIPSModel(seqsForAlignment, ipsSiteModel, 1, 1, null);
        return matrix;
    }

    private static IPSSiteModel[] splitMatrixBasedTwoMaximalFrequencies(IPSSiteModel model)
    {
        List<IPSSiteModel> result = new ArrayList<>();
        FrequencyMatrix matrix = model.getMatrices()[0].getFrequencyMatrix();
        for (int i=0; i<matrix.getLength(); i++)
        {
            double freq1 = 0;
            double freq2 = 0;
            byte[] codes = matrix.getAlphabet().basicCodes();
            byte codeMax1 = 0, codeMax2 = 0;
            for(byte code : codes)
            {
                double frequency = matrix.getFrequency(i, code);
                if (freq1<frequency) {freq1=frequency; codeMax1=code;}
            }
            for(byte code : codes)
            {
                double frequency = matrix.getFrequency(i, code);
                if (freq2<frequency && code!=codeMax1) {freq2=frequency; codeMax2=code;}
            }
            if (freq1<MAX_THRESHOLD)
            {
                FrequencyMatrix matrix1 = new FrequencyMatrix(matrix.getOrigin(), matrix.getName() + "_1", matrix);
                FrequencyMatrix matrix2 = new FrequencyMatrix(matrix.getOrigin(), matrix.getName() + "_2", matrix);
                for(byte code : codes)
                {
                    matrix1.setFrequency(i, code, 0.0);
                }
                matrix1.setFrequency(i, codeMax1, 1.0);
                for(byte code : codes)
                {
                    matrix2.setFrequency(i, code, 0.0);
                }
                matrix2.setFrequency(i, codeMax2, 1.0);
                
                
                IPSSiteModel subModel = new IPSSiteModel(model.getName() + "_" + i, null, new FrequencyMatrix[]{matrix1, matrix2}, model.getThreshold(),
                        model.getDistMin(), model.getWindow());
                
                result.add(subModel);
            }
        }
            
        return result.toArray(new IPSSiteModel[result.size()]);
    }

    private static boolean alignmentsEqual(Site[] a, Site[] b)
    {
        if( a == b )
            return true;
        if( a == null || b == null )
            return false;
        if( a.length != b.length )
            return false;
        for( int i = 0; i < a.length; i++ )
        {
            Site s1 = a[i];
            Site s2 = b[i];
            if( s1 == s2 )
                continue;
            if( s1 == null || s2 == null )
                return false;
            if( s1.getStart() != s2.getStart() || s1.getStrand() != s2.getStrand() )
                return false;
        }
        return true;
    }

    private static Site[] searchBestSites(IPSSiteModel model, List<Sequence> sequences)
    {
        Site[] sites = new Site[sequences.size()];
        for( int i = 0; i < sequences.size(); i++ )
        {
            Sequence sequence = sequences.get(i);
            Site forwardSite = model.findBestSite(sequence);
            Sequence reverseSequence = SequenceRegion.getReversedSequence( sequence );
            Site reverseSite = model.findBestSite(reverseSequence);
            if( ( forwardSite == null ) || ( reverseSite == null ) ) //sequence to short for model
                continue;
            double forwardScore = forwardSite.getScore();
            double reverseScore = reverseSite.getScore();
            if( Math.max(forwardScore, reverseScore) < model.getThreshold() )
                continue;
            sites[i] = ( forwardScore >= reverseScore ) ? forwardSite : reverseSite;
        }
        return sites;
    }

    private static void updateIPSModel(IPSSiteModel model, Site[] sites, int minClusterSize) throws Exception
    {
        WeightMatrixModel[] matrices = StreamEx.of(model.getMatrices())
            .mapToEntry( wm -> StreamEx.of( sites ).nonNull()
                    .filter( s -> s.getScore() >= model.getThreshold() )
                    .filter( s -> model.getMatrices()[(Integer)s.getProperties().getValue(IPSSiteModel.MATRIX_MODEL_PROPERTY)] == wm )
                    .map( Site::getSequence )
                    .toList() )
            .filterValues( sequences -> sequences.size() >= minClusterSize )
            .peekKeyValue( (wm, sequences) -> {
                FrequencyMatrix pfm = wm.getFrequencyMatrix();
                pfm.updateFromSequences(sequences);
                wm.setFrequencyMatrix(pfm);
                wm.setThreshold(wm.getMaxScore() / 2);
            }).keys().toArray( WeightMatrixModel[]::new );
        if( matrices.length == 0 )
            throw new Exception("Can not construct IPS site model, try to decrease critIPS");
        model.setMatrices(matrices);
    }
}

package biouml.plugins.perfectosape;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import biouml.standard.type.Species;
import ru.autosome.perfectosape.backgroundModels.BackgroundModel;
import ru.autosome.perfectosape.BoundaryType;
import ru.autosome.perfectosape.SequenceWithSNP;
import ru.autosome.perfectosape.backgroundModels.WordwiseBackground;
import ru.autosome.perfectosape.api.MultiSNPScan;
import ru.autosome.perfectosape.api.PrecalculateThresholdLists;
import ru.autosome.perfectosape.api.Task;
import ru.autosome.perfectosape.calculations.SNPScan.RegionAffinityInfos;
import ru.autosome.perfectosape.calculations.findPvalue.CanFindPvalue;
import ru.autosome.perfectosape.motifModels.PPM;
import ru.autosome.perfectosape.motifModels.PWM;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.access.security.SessionThreadFactory;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.VariationElement;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * @author lan
 *
 */
@CodePrivilege(CodePrivilegeType.THREAD)
public class PerfectosapeAnalysis extends AnalysisMethodSupport<PerfectosapeAnalysisParameters>
{
    public PerfectosapeAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new PerfectosapeAnalysisParameters());
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info("Reading matrices...");
        List<PWM> pwms = readPWMs();
        jobControl.setPreparedness(1);
        if( jobControl.isStopped() )
            return null;

        log.info("Reading sequences table...");
        Map<SequenceWithSNP, String> seqNames = getSequences();
        jobControl.setPreparedness(2);
        if(jobControl.isStopped())
            return null;

        log.info("Calculating thresholds...");
        double[] pValues = ru.autosome.perfectosape.calculations.PrecalculateThresholdList.PVALUE_LIST;
        double discretization = 10000;
        BackgroundModel background = new WordwiseBackground();
        BoundaryType pvalueBoundary = BoundaryType.LOWER;
        int maxHashSize = 10000000;
        

        PrecalculateThresholdLists.Parameters listCalculationParams = new PrecalculateThresholdLists.Parameters();//(pwms, pValues, discretization, background, pvalueBoundary, maxHashSize);
        listCalculationParams.pwmCollection = pwms;
        listCalculationParams.pvalues = pValues;
        listCalculationParams.discretization = discretization;
        listCalculationParams.background = background;
        listCalculationParams.pvalue_boundary = pvalueBoundary;
        listCalculationParams.max_hash_size = maxHashSize;
        
        PrecalculateThresholdLists listCalculator = new PrecalculateThresholdLists(listCalculationParams);
        jobControl.pushProgress(2, 50);
        Map<PWM, CanFindPvalue> pwmCollectionWithPvalueCalculators = runTask(listCalculator);
        jobControl.popProgress();
        if(jobControl.isStopped())
            return null;

        log.info("Scanning SNPs...");
        jobControl.pushProgress(50, 80);
        List<SequenceWithSNP> seqs = new ArrayList<>( seqNames.keySet() );
        MultiSNPScan.Parameters scan_parameters = new MultiSNPScan.Parameters(seqs, pwmCollectionWithPvalueCalculators);
        MultiSNPScan scan_calculator = new MultiSNPScan(scan_parameters);
        Map<PWM, Map<SequenceWithSNP, RegionAffinityInfos>> results = runTask(scan_calculator);
        jobControl.popProgress();
        if(jobControl.isStopped())
            return null;

        log.info("Storing result...");
        jobControl.pushProgress(80, 100);
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection(parameters.getOutTable());
        result.getInfo().getProperties().setProperty(TableDataCollection.GENERATED_IDS, "true");
        result.getInfo().getProperties().setProperty(TableDataCollection.INTEGER_IDS, "true");
        ColumnModel columnModel = result.getColumnModel();
        columnModel.addColumn("Matrix", DataElementPath.class);
        columnModel.addColumn("SNP", String.class);
        columnModel.addColumn("Min p-value", Double.class);
        columnModel.addColumn("Fold change", Double.class);
        columnModel.addColumn("Position 1", Integer.class);
        columnModel.addColumn("Strand 1", String.class);
        columnModel.addColumn("Sequence 1", String.class);
        columnModel.addColumn("Position 2", Integer.class);
        columnModel.addColumn("Strand 2", String.class);
        columnModel.addColumn("Sequence 2", String.class);
        columnModel.addColumn("P-value 1", Double.class);
        columnModel.addColumn("P-value 2", Double.class);

        int idx = 1;
        for(Entry<PWM, Map<SequenceWithSNP, RegionAffinityInfos>> entry : results.entrySet())
        {
            for(Entry<SequenceWithSNP, RegionAffinityInfos> pwmEntry : entry.getValue().entrySet())
            {
                RegionAffinityInfos infos = pwmEntry.getValue();
                Object[] rowData = new Object[columnModel.getColumnCount()];
                int pos = 0;
                rowData[pos++] = parameters.getMatrixLib().getChildPath(entry.getKey().name);
                rowData[pos++] = seqNames.get(pwmEntry.getKey());
                rowData[pos++] = Math.min(infos.getInfo_1().getPvalue(), infos.getInfo_2().getPvalue());
                rowData[pos++] = infos.foldChange();
                rowData[pos++] = infos.getInfo_1().getPosition().position;
                rowData[pos++] = infos.getInfo_1().getPosition().directStrand ? "+" : "-";
                rowData[pos++] = infos.getInfo_1().getWord().toString();
                rowData[pos++] = infos.getInfo_2().getPosition().position;
                rowData[pos++] = infos.getInfo_2().getPosition().directStrand ? "+" : "-";
                rowData[pos++] = infos.getInfo_2().getWord().toString();
                rowData[pos++] = infos.getInfo_1().getPvalue();
                rowData[pos++] = infos.getInfo_2().getPvalue();
                TableDataCollectionUtils.addRow(result, String.valueOf(idx++), rowData, true);
            }
        }
        result.finalizeAddition();
        CollectionFactoryUtils.save(result);
        jobControl.popProgress();
        return result;
    }

    private Map<SequenceWithSNP, String> getSequences() throws Exception
    {
        Map<SequenceWithSNP, String> seqNames = new IdentityHashMap<>();
        if(parameters.getMode().equals(PerfectosapeAnalysisParameters.SEQUENCES_MODE))
        {
            TableDataCollection table = parameters.getSeqTable().getDataElement(TableDataCollection.class);
            ColumnModel model = table.getColumnModel();
            boolean multiColumnMode;
            if( model.getColumnCount() >= 3 )
            {
                log.info("Reading column '" + model.getColumn(0).getName() + "' as left flank; column '" + model.getColumn(1).getName()
                        + "' as SNP list; column '" + model.getColumn(2).getName() + "' as right flank");
                multiColumnMode = true;
            }
            else if( model.getColumnCount() >= 1 )
            {
                log.info("Reading column '" + model.getColumn(0).getName() + "' as sequence with SNP");
                multiColumnMode = false;
            }
            else
                throw new Exception("Table should contain at least one column");
            for(RowDataElement row : table)
            {
                Object[] values = row.getValues();
                SequenceWithSNP seq;
                if(multiColumnMode)
                {
                    seq = new SequenceWithSNP(String.valueOf(values[0]), String.valueOf(values[1]).toCharArray(), String.valueOf(values[2]));
                } else
                {
                    seq = SequenceWithSNP.fromString(String.valueOf(values[0]));
                }
                seqNames.put(seq, row.getName());
            }
        } else
        {
            Species species = parameters.getSpecies();
            DataElementPath ensemblPath = TrackUtils.getEnsemblPath(species, parameters.getOutTable());
            DataCollection<VariationElement> variations = ensemblPath.getChildPath("Data", "variation").getDataCollection(VariationElement.class);
            DataCollection<AnnotatedSequence> sequences = TrackUtils.getPrimarySequencesPath(ensemblPath).getDataCollection(AnnotatedSequence.class);
            TableDataCollection snpTable = parameters.getSnpTable().getDataElement(TableDataCollection.class);
            for(String snp : snpTable.getNameList())
            {
                VariationElement element = variations.get(snp);
                if(element == null)
                {
                    log.warning("SNP "+snp+" not found in "+ensemblPath+"; skipping");
                    continue;
                }
                AnnotatedSequence sequence = sequences.get(element.getSite().getOriginalSequence().getName());
                if(sequence == null)
                {
                    log.warning("Sequence "+element.getSite().getOriginalSequence().getName()+" not found in "+sequences.getCompletePath()+"; skipping");
                    continue;
                }
                Interval interval = new Interval(element.getSite().getFrom()).zoomToLength(100).fit(sequence.getSequence().getInterval());
                String seqString = new SequenceRegion(sequence.getSequence(), interval, false, false).toString();
                seqString = seqString.substring(0, element.getSite().getFrom()-interval.getFrom())+"["+element.getAllele()+"]"+seqString.substring(element.getSite().getTo()-interval.getFrom()+1);
                seqNames.put(SequenceWithSNP.fromString(seqString), snp);
            }
        }
        return seqNames;
    }

    private List<PWM> readPWMs()
    {
        List<PWM> pwms = new ArrayList<>();
        for(FrequencyMatrix matrix : parameters.getMatrixLib().getDataCollection(FrequencyMatrix.class))
        {
            try
            {
                pwms.add(convertMatrixToPWM(matrix));
            }
            catch( IllegalArgumentException e )
            {
                log.warning(e.getMessage()+"; skipping");
            }
        }
        return pwms;
    }

    private <T> T runTask(final Task<T> task)
    {
        ExecutorService pool = Executors.newFixedThreadPool(1, new SessionThreadFactory());
        try
        {
            Future<T> future = pool.submit(task);
            while(true)
            {
                try
                {
                    return future.get(1, TimeUnit.SECONDS);
                }
                catch( InterruptedException | ExecutionException e )
                {
                    throw ExceptionRegistry.translateException(e);
                }
                catch( TimeoutException e )
                {
                }
                jobControl.setPreparedness((int)task.completionPercent());
                if(jobControl.isStopped())
                    task.setStatus(Task.Status.INTERRUPTED);
            }
        }
        finally
        {
            pool.shutdownNow();
        }
    }

    /**
     * @param matrix
     * @return
     * @throws Exception
     */
    private PWM convertMatrixToPWM(FrequencyMatrix matrix) throws IllegalArgumentException
    {
        if(!matrix.getAlphabet().isNucleotide())
        {
            throw new IllegalArgumentException("Matrix "+matrix.getName()+" has non-nucleotide alphabet");
        }
        if(matrix.getAlphabet().codeLength() != 1)
        {
            throw new IllegalArgumentException("Matrix "+matrix.getName()+" is not mono-nucleotide matrix");
        }
        double[][] pwmData = new double[matrix.getLength()][];
        for(int i=0; i<matrix.getLength(); i++)
        {
            double[] pwmDataRow = new double[4];
            for(byte j=0; j<4; j++)
            {
                pwmDataRow[j] = matrix.getFrequency(i, j);
            }
            pwmData[i] = pwmDataRow;
        }
        return new PPM(pwmData, matrix.getName()).to_pwm(new WordwiseBackground(), 100);
    }
}

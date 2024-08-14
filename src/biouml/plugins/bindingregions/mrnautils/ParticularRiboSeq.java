
package biouml.plugins.bindingregions.mrnautils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.bindingregions.utils.EnsemblUtils;
import biouml.plugins.bindingregions.utils.EnsemblUtils.Gene;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.Olig;
import biouml.plugins.bindingregions.utils.TableUtils;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.exception.InternalException;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * @author yura
 * class is designed to handle Ribo-Seq data sets with distinct formats and reduce them into 'List<GeneTranscript> geneTranscriptList'
 */

public class ParticularRiboSeq
{
    public static final String TWO_TABLES_FOR_TE = "Two tables for calculation of translation efficiency (TE)";
    public static final String TWO_TABLES_FOR_TIE = "Two tables for calculation of translation initiation efficiency (TIE)";
    public static final String INGOLIA_GSE30839_DATA_SET = "Set1: GSE30839 (Ingolia, mouse)_" + Gene.PROTEIN_CODING;
    private static final String REID_GSE31539_DATA_SET_1 = "Set2: GSE31539 (Reid, human), 1-st version_" + Gene.PROTEIN_CODING;
    private static final String REID_GSE31539_DATA_SET_2 = "Set2_2: GSE31539 (Reid, human), 2-nd version_" + Gene.PROTEIN_CODING;
    private static final String INGOLIA_GSE37744_A_DATA_SET = "Set3_A: GSE37744_A (Ingolia, human)_" + Gene.PROTEIN_CODING;
    private static final String INGOLIA_GSE37744_B_DATA_SET = "Set3_B: GSE37744_B (Ingolia, human)_" + Gene.PROTEIN_CODING;
    private static final String INGOLIA_GSE37744_C_DATA_SET = "Set3_C: GSE37744_C (Ingolia, human)_" + Gene.PROTEIN_CODING;
    public static final String ALL_TRANSCRIPTS_WITH_PROTEIN_CODING_SET = "All " + Gene.PROTEIN_CODING + " transcripts available in Ensembl";
    public static final String ALL_PROTEIN_CODING_WITH_PREDICTED_CDS = "All " + Gene.PROTEIN_CODING + " transcripts available in Ensembl with predicted CDSs";
    public static final String ALL_TRANSCRIPTS_WITH_LINC_RNA = "All lincRNA transcripts available in Ensembl";
    
    private static final String FEATURE_1_R1 = "R1, reads number (harringtonine-treatment)";
    private static final String FEATURE_1_R1_SHORT = "Rs, reads number";
    private static final String FEATURE_2_R2 = "R2, reads per base (cycloheximide-treatment)";
    private static final String FEATURE_3_RATIO_R1_R2 = "Ratio R1:R2";
    private static final String FEATURE_4_LG_R1 = "lg(R1)";
    private static final String FEATURE_5_RATIO_LG_R1_R2 = "Ratio lg(R1):R2";
    private static final String FEATURE_4_LG_Rs = "lg(Rs)";
    private static final String FEATURE_6_TOTAL_TRLN_EFFICIENCY = "Total translation efficiency";
    private static final String FEATURE_7_MEM_TRLN_EFFICIENCY = "Mem translation efficiency";
    private static final String FEATURE_8_CYT_TRLN_EFFICIENCY = "Cyt translation efficiency";
    private static final String FEATURE_9_TOTAL_RIBO_RPKM = "Total ribo RPKM";
    private static final String FEATURE_10_MEM_RIBO_RPKM = "Mem ribo RPKM";
    private static final String FEATURE_11_CYT_RIBO_RPKM = "Cyt ribo RPKM";
    private static final String FEATURE_12_LG_TOTAL_RIBO_RPKM = "lg(" + FEATURE_9_TOTAL_RIBO_RPKM + ")";
    private static final String FEATURE_13_LG_MEM_RIBO_RPKM = "lg(" + FEATURE_10_MEM_RIBO_RPKM + ")";
    private static final String FEATURE_14_LG_CYT_RIBO_RPKM = "lg(" + FEATURE_11_CYT_RIBO_RPKM + ")";
    private static final String FEATURE_15_PEAK_SCORE = "Peak score";
    private static final String FEATURE_17_R3 = "R3, Ribosome density";
    private static final String FEATURE_18_R4 = "R4, mRNA density";
    private static final String FEATURE_19_TE = "TE, Translation efficiency = R3:R4";
    private static final String FEATURE_20_LG_R3 = "lg(R3)";
    private static final String FEATURE_21_LG_R4 = "lg(R4)";
    private static final String FEATURE_22_LG_TE = "lg(TE)";
    private static final String FEATURE_23_GIVEN_TE = "given TE, given translation efficiency";
    private static final String FEATURE_24_LG_GIVEN_TE = "lg(given TE)";
    private static final String FEATURE_25_RS_CYCLOHEXIMIDE = "Rs_Cycl = Number of reads from Ribo-Seq after cycloheximide-like treatment";
    private static final String FEATURE_26_RS_CYCLOHEXIMIDE_NORMALIZED = "Rs_Cycl_Normalized = Rs_Cycl : CDS length";
    private static final String FEATURE_26_RS_CYCLOHEXIMIDE_NORMALIZED_LG = "lg(Rs_Cycl_Normalized)";
    private static final String FEATURE_27_RS_MRNA = "Rs_mRNA = Number of reads from mRNA-Seq";
    private static final String FEATURE_28_RS_MRNA_NORMALIZED = "Rs_mRNA_Normalized = Rs_mRNA : transcript length";
    private static final String FEATURE_28_RS_MRNA_NORMALIZED_LG = "lg(Rs_mRNA_Normalized)";
    private static final String FEATURE_29_TE = "TE (Translation Efficiency) = Rs_Cycl_Normalized : Rs_mRNA_Normalized";
    private static final String FEATURE_30_RS_HARRINGTONINE = "Rs_Harr = Number of reads from Ribo-Seq after harringtonine-like treatment";
    private static final String FEATURE_31_RS_HARRINGTONINE_LG = "lg(Rs_Harr)";
    private static final String FEATURE_32_TIE = "TIE (Translation Initiation Efficiency) = Rs_Harr / Rs_mRNA_Normalized";
    private static final String FEATURE_33_TIE_LG = "TIE_lg = lg(Rs_Harr) / Rs_mRNA_Normalized";
    
    private final String dataSetName;
    private DataElementPath pathToData;
    private List<GeneTranscript> geneTranscriptList;
    private DataElementPath pathToSequences; // path to genome chromosomes or path to FASTA format

    public ParticularRiboSeq(String dataSetName, DataElementPath pathToFolderWithDataSets, DataElementPath pathToTableWithMrnaSeqData, String columnNameWithMrnaSeqReadsNumber, double mrnaSeqReadsThreshold, DataElementPath pathToTableWithRiboSeqData, String columnNameWithRiboSeqReadsNumber, double riboSeqReadsThreshold, String columnNameWithStartCodonPositions, String columnNameWithTranscriptNames, DataElementPath pathToSequences, String startCodonType, int orderOfStartCodon, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        this.dataSetName = dataSetName;
        String[] givenTranscriptNames = null;
        switch( dataSetName )
        {
            case TWO_TABLES_FOR_TE                       : this.pathToSequences = pathToSequences;
                                                           Map<String, double[]> transcriptNameAndMrnaAndRiboSeqReads = getTranscriptNameAndMrnaAndRiboSeqReads(pathToTableWithMrnaSeqData, columnNameWithMrnaSeqReadsNumber, mrnaSeqReadsThreshold, pathToTableWithRiboSeqData, columnNameWithRiboSeqReadsNumber, riboSeqReadsThreshold, jobControl, from, from + (to - from) / 3);
                                                           givenTranscriptNames = transcriptNameAndMrnaAndRiboSeqReads.keySet().toArray(new String[0]);
                                                           this.geneTranscriptList = GeneTranscript.readTranscriptsInEnsembl(givenTranscriptNames, this.pathToSequences, jobControl, from + (to - from) / 3, from + 2 * (to - from) / 3);
                                                           this.geneTranscriptList = GeneTranscript.removeNonProteinCodingTranscriptsWithShortLiders(geneTranscriptList, jobControl, from + 2 * (to - from) / 3, from + 3 * (to - from) / 4);
                                                           setValuesOfMrnaAndRiboSeqFeaturesFromTwoTablesForTE(transcriptNameAndMrnaAndRiboSeqReads, jobControl, from + 3 * (to - from) / 4, to); break;
            case TWO_TABLES_FOR_TIE                      : this.pathToSequences = pathToSequences;
                                                           this.geneTranscriptList = getGeneTranscriptListInTableForTIE(this.pathToSequences, pathToTableWithRiboSeqData, columnNameWithRiboSeqReadsNumber, riboSeqReadsThreshold, columnNameWithStartCodonPositions, columnNameWithTranscriptNames, pathToTableWithMrnaSeqData, columnNameWithMrnaSeqReadsNumber, mrnaSeqReadsThreshold, jobControl, from, from +  9 * (to - from) / 10);
                                                           this.geneTranscriptList = GeneTranscript.removeNonProteinCodingTranscriptsWithShortLiders(geneTranscriptList, jobControl, from +  9 * (to - from) / 10, 95 * (to - from) / 100); break;
            case INGOLIA_GSE30839_DATA_SET               : this.pathToData = pathToFolderWithDataSets.getChildPath("DAT1");
                                                           this.pathToSequences = pathToData.getChildPath("old_seq_ingolia");
                                                           Map<String, Double> transcriptsNamesAndCommonRiboseqValuesForWholeTranscripts = TableUtils.readGivenColumnInDoubleTableAsMap(pathToData.getChildPath("tableS1"), "footprintsOnbase");
                                                           if( jobControl != null )
                                                               jobControl.setPreparedness(from + (to - from) / 5);
                                                           Map<String, List<Integer>> transcriptNameAndIndecesOfRows = readMrnaNamesAndIndecesOfRows(pathToData.getChildPath("tableS3_2"), "UCSC_ID");
                                                           givenTranscriptNames = transcriptNameAndIndecesOfRows.keySet().toArray(new String[0]);
                                                           Map<String, double[]> transcriptNameAndRiboseqDensityAndMrnaDensity = readTranscriptNameAndRiboseqDensityAndMrnaDensityAndTEInTable(pathToData, givenTranscriptNames, jobControl, from + (to - from) / 5, from + (to - from) / 2);
                                                           this.geneTranscriptList = getGeneTranscriptListFromParticularIngoliaGSE30839DataSet(pathToData, transcriptsNamesAndCommonRiboseqValuesForWholeTranscripts , transcriptNameAndIndecesOfRows, transcriptNameAndRiboseqDensityAndMrnaDensity, jobControl, from + (to - from) / 2, to); break;
            case REID_GSE31539_DATA_SET_1                :
            case REID_GSE31539_DATA_SET_2                : this.pathToData = dataSetName.equals(REID_GSE31539_DATA_SET_1) ? pathToFolderWithDataSets.getChildPath("DAT2", "GSE31539_processed_data Transcripts Ens_2") : pathToFolderWithDataSets.getChildPath("DAT2_2", "Reidarticle_RefSeq Transcripts Ensembl");
                                                           this.pathToSequences = pathToSequences;
                                                           List<String> transcriptNames = TableUtils.readRowNamesInTable(pathToData);
                                                           this.geneTranscriptList = GeneTranscript.readTranscriptsInEnsembl(transcriptNames.toArray(new String[0]), this.pathToSequences, jobControl, from, from + (to - from) / 2);
                                                           this.geneTranscriptList = GeneTranscript.removeNonProteinCodingTranscriptsWithShortLiders(geneTranscriptList, jobControl, from + (to - from) / 2, to);
                                                           setValuesOfRibiSeqFeaturesForParticularGSE31539DataSet(); break;
            case INGOLIA_GSE37744_A_DATA_SET             :
            case INGOLIA_GSE37744_B_DATA_SET             :
            case INGOLIA_GSE37744_C_DATA_SET             : this.pathToData = pathToFolderWithDataSets.getChildPath("DAT3");
                                                           this.pathToSequences = pathToSequences;
                                                           DataElementPath dep = null;
                                                           if( dataSetName.equals(INGOLIA_GSE37744_A_DATA_SET) )
                                                               dep = pathToData.getChildPath("GSM926674_101209A_qexpr");
                                                           else if( dataSetName.equals(INGOLIA_GSE37744_B_DATA_SET) )
                                                               dep = pathToData.getChildPath("GSM926675_101209B_qexpr");
                                                           else if( dataSetName.equals(INGOLIA_GSE37744_C_DATA_SET) )
                                                               dep = pathToData.getChildPath("GSM926676_101209C_qexpr");
                                                           else
                                                               throw new InternalException("Unexpected data set name: " + dataSetName);
                                                           List<String> transcriptsNames = TableUtils.readRowNamesInTable(dep);
                                                           filtrationOfTranscriptNamesForIngolia_GSE37744(transcriptsNames, jobControl, from, (from + to) / 2);
                                                           Map<String, Double> transcriptsNamesAndRiboseqValues = TableUtils.readGivenColumnInDoubleTableAsMap(dep, "The raw count of footprint reads whose A site maps to the coding sequence in the gene model");
                                                           this.geneTranscriptList = getGeneTranscriptListForIngoliaGSE37744DataSet(transcriptsNames, transcriptsNamesAndRiboseqValues, jobControl, (from + to) / 2, to); break;
            case ALL_TRANSCRIPTS_WITH_PROTEIN_CODING_SET : this.pathToSequences = pathToSequences;
                                                           givenTranscriptNames = GeneTranscript.getNamesOfProteinCodingTranscriptsInEnsembl(this.pathToSequences, jobControl, from, from + (to - from) / 2);
                                                           this.geneTranscriptList = GeneTranscript.readTranscriptsInEnsembl(givenTranscriptNames, this.pathToSequences, jobControl, from + (to - from) / 2, from + 5 * (to - from) / 6);
                                                           this.geneTranscriptList = GeneTranscript.removeNonProteinCodingTranscriptsWithShortLiders(geneTranscriptList, jobControl, from + 5 * (to - from) / 6,  to); break;
            case ALL_PROTEIN_CODING_WITH_PREDICTED_CDS   : this.pathToSequences = pathToSequences;
                                                           givenTranscriptNames = GeneTranscript.getNamesOfProteinCodingTranscriptsInEnsembl(this.pathToSequences, jobControl, from, from + (to - from) / 3);
                                                           this.geneTranscriptList = GeneTranscript.readTranscriptsInEnsembl(givenTranscriptNames, this.pathToSequences, jobControl, (to - from) / 3, 2 * (to - from) / 3);
                                                           getRNAsWithHypotheticalCDSs(this.pathToSequences, orderOfStartCodon, jobControl, 2 * (to - from) / 3, to); break;
            case ALL_TRANSCRIPTS_WITH_LINC_RNA           : this.pathToSequences = pathToSequences;
                                                           this.geneTranscriptList = GeneTranscript.readTranscriptsInEnsembl(null, this.pathToSequences, jobControl, from, from + (to - from) / 2);
                                                           selectLincRnaTranscripts(jobControl, from + (to - from) / 2, from +  3 * (to - from) / 4);
                                                           getLincRNAswithHypotheticalCDS(this.pathToSequences, orderOfStartCodon, jobControl, from +  3 * (to - from) / 4, to); break;
            default                                      : throw new IllegalArgumentException(dataSetName);
        }
        if( ! dataSetName.equals(ALL_TRANSCRIPTS_WITH_LINC_RNA) && ! dataSetName.equals(ALL_PROTEIN_CODING_WITH_PREDICTED_CDS) )
            selectTranscriptsWithGivenStartCodonType(startCodonType);
    }
    
    ////////////////////////////////0000//////////////////////////
    public static StreamEx<String> getAvailableRiboSeqFeatureNames(String dataSetName)
    {
        switch( dataSetName )
        {
            case TWO_TABLES_FOR_TE           : return StreamEx.of(FEATURE_25_RS_CYCLOHEXIMIDE, FEATURE_26_RS_CYCLOHEXIMIDE_NORMALIZED, FEATURE_26_RS_CYCLOHEXIMIDE_NORMALIZED_LG, FEATURE_27_RS_MRNA, FEATURE_28_RS_MRNA_NORMALIZED, FEATURE_28_RS_MRNA_NORMALIZED_LG, FEATURE_29_TE, FEATURE_22_LG_TE);
            case TWO_TABLES_FOR_TIE          : return StreamEx.of(FEATURE_30_RS_HARRINGTONINE, FEATURE_31_RS_HARRINGTONINE_LG, FEATURE_27_RS_MRNA, FEATURE_28_RS_MRNA_NORMALIZED, FEATURE_32_TIE, FEATURE_33_TIE_LG);
            case INGOLIA_GSE30839_DATA_SET   : return StreamEx.of(FEATURE_1_R1, FEATURE_2_R2, FEATURE_17_R3, FEATURE_18_R4, FEATURE_15_PEAK_SCORE, FEATURE_3_RATIO_R1_R2, FEATURE_4_LG_R1, FEATURE_5_RATIO_LG_R1_R2, FEATURE_19_TE, FEATURE_20_LG_R3, FEATURE_21_LG_R4, FEATURE_22_LG_TE, FEATURE_23_GIVEN_TE, FEATURE_24_LG_GIVEN_TE);
            case REID_GSE31539_DATA_SET_1    :
            case REID_GSE31539_DATA_SET_2    : return StreamEx.of(FEATURE_6_TOTAL_TRLN_EFFICIENCY, FEATURE_7_MEM_TRLN_EFFICIENCY, FEATURE_8_CYT_TRLN_EFFICIENCY, FEATURE_9_TOTAL_RIBO_RPKM, FEATURE_10_MEM_RIBO_RPKM, FEATURE_11_CYT_RIBO_RPKM, FEATURE_12_LG_TOTAL_RIBO_RPKM, FEATURE_13_LG_MEM_RIBO_RPKM, FEATURE_14_LG_CYT_RIBO_RPKM);
            case INGOLIA_GSE37744_A_DATA_SET :
            case INGOLIA_GSE37744_B_DATA_SET :
            case INGOLIA_GSE37744_C_DATA_SET : return StreamEx.of(FEATURE_1_R1_SHORT, FEATURE_4_LG_Rs);
            default                          : return StreamEx.empty();
        }
    }
    
    public static String[] getAllAvailableDataSetNames()
    {
        return new String[]{TWO_TABLES_FOR_TE, TWO_TABLES_FOR_TIE, ALL_TRANSCRIPTS_WITH_PROTEIN_CODING_SET, ALL_PROTEIN_CODING_WITH_PREDICTED_CDS, ALL_TRANSCRIPTS_WITH_LINC_RNA, INGOLIA_GSE30839_DATA_SET, REID_GSE31539_DATA_SET_1, REID_GSE31539_DATA_SET_2, INGOLIA_GSE37744_A_DATA_SET, INGOLIA_GSE37744_B_DATA_SET, INGOLIA_GSE37744_C_DATA_SET};
    }
    
    public static String[] getNamesOfDataSetsWithRiboseqFeatures()
    {
        return new String[]{TWO_TABLES_FOR_TE, TWO_TABLES_FOR_TIE, REID_GSE31539_DATA_SET_1, REID_GSE31539_DATA_SET_2, INGOLIA_GSE37744_A_DATA_SET, INGOLIA_GSE37744_B_DATA_SET, INGOLIA_GSE37744_C_DATA_SET};
    }

    private void selectTranscriptsWithGivenStartCodonType(String startCodonType) throws Exception
    {
        List<GeneTranscript> list = new ArrayList<>();
        switch( startCodonType )
        {
            default                                      :
            case GeneTranscript.EACH_START_CODON         : return;
            case GeneTranscript.CANONICAL_START_CODON    : for( GeneTranscript gt : geneTranscriptList )
                                                               if( gt.isStartCodonCanonical(pathToSequences) )
                                                                   list.add(gt);
                                                           break;
            case GeneTranscript.NONCANONICAL_START_CODON : for( GeneTranscript gt : geneTranscriptList )
                                                               if( ! gt.isStartCodonCanonical(pathToSequences) )
                                                                   list.add(gt);
                                                           break;
        }
        geneTranscriptList = list;
    }

    // TODO: it is temporarily available; to remove after refactoring 'GeneTranscript.searchForPerfectHairpins()'
    public List<GeneTranscript> getGeneTranscriptList()
    {
        return geneTranscriptList;
    }
    
    // TODO: it is temporarily available; to remove after refactoring 'GeneTranscript.searchForPerfectHairpins()'
    public DataElementPath getPathToSequences()
    {
        return pathToSequences;
    }

    private Map<String, double[]> getTranscriptNameAndMrnaAndRiboSeqReads(DataElementPath pathToTableWithMrnaSeqData, String columnNameWithMrnaSeqReadsNumber, double mrnaSeqReadsThreshold, DataElementPath pathToTableWithRiboSeqData, String columnNameWithRiboSeqReadsNumber, double riboSeqReadsThreshold, AnalysisJobControl jobControl, int from, int to)
    {
        Map<String, double[]> result = new HashMap<>();
        Map<String, Double> transcriptNameAndMrnaSeqReads = TableUtils.readGivenColumnInDoubleTableAsMap(pathToTableWithMrnaSeqData, columnNameWithMrnaSeqReadsNumber);
        Map<String, Double> transcriptNameAndRiboSeqReads = TableUtils.readGivenColumnInDoubleTableAsMap(pathToTableWithRiboSeqData, columnNameWithRiboSeqReadsNumber);
        int difference = to - from, iJobControl = 0, n = transcriptNameAndMrnaSeqReads.size();
        for( Entry<String, Double> entry : transcriptNameAndMrnaSeqReads.entrySet() )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + ++iJobControl * difference / n);
            String transcriptName = entry.getKey();
            double mrnaSeqReads = entry.getValue();
            if( mrnaSeqReads < mrnaSeqReadsThreshold ) continue;
            Double riboSeqReads = transcriptNameAndRiboSeqReads.get(transcriptName);
            if( riboSeqReads == null || riboSeqReads < riboSeqReadsThreshold ) continue;
            result.put(transcriptName, new double[]{mrnaSeqReads, riboSeqReads});
        }
        if( result.isEmpty() ) return null;
        return result;
    }

    private void setValuesOfMrnaAndRiboSeqFeaturesFromTwoTablesForTE(Map<String, double[]> transcriptNameAndMrnaAndRiboSeqReads, AnalysisJobControl jobControl, int from, int to)
    {
        int difference = to - from;
        for( int i = 0; i < geneTranscriptList.size(); i++ )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / geneTranscriptList.size());
            Map<String, Double> map = new HashMap<>();
            GeneTranscript gt = geneTranscriptList.get(i);
            double[] mrnaAndRiboSeqReads = transcriptNameAndMrnaAndRiboSeqReads.get(gt.getTranscriptName());
            if( mrnaAndRiboSeqReads == null ) continue;
            map.put(FEATURE_25_RS_CYCLOHEXIMIDE, mrnaAndRiboSeqReads[1]);
            double riboNormalized = mrnaAndRiboSeqReads[1] / gt.getCdsLength();
            map.put(FEATURE_26_RS_CYCLOHEXIMIDE_NORMALIZED, riboNormalized);
            double lgRiboNormalized = riboNormalized <= 0.0 ? Double.NaN : Math.log10(riboNormalized);
            map.put(FEATURE_26_RS_CYCLOHEXIMIDE_NORMALIZED_LG, lgRiboNormalized);
            map.put(FEATURE_27_RS_MRNA, mrnaAndRiboSeqReads[0]);
            double mrnaNormalized = mrnaAndRiboSeqReads[0] / gt.getTranscriptLength();
            map.put(FEATURE_28_RS_MRNA_NORMALIZED, mrnaNormalized);
            double lgMrnaNormalized = mrnaNormalized <= 0.0 ? Double.NaN : Math.log10(mrnaNormalized);
            map.put(FEATURE_28_RS_MRNA_NORMALIZED_LG, lgMrnaNormalized);
            double te = mrnaAndRiboSeqReads[0] == 0.0 ? Double.NaN : mrnaAndRiboSeqReads[1] / mrnaAndRiboSeqReads[0];
            map.put(FEATURE_29_TE, te);
            double lgTE = Double.isNaN( te ) ? Double.NaN : Math.log10( te );
            map.put(FEATURE_22_LG_TE, lgTE);
            gt.setNameAndValueOfRiboSeqFeature(map);
        }
    }

    // Usually 'columnNameWithStartCodonPositions' = "Summit offset"
    private Map<String, List<Object[]>> readTranscriptNamesAndObjectsInTableForTIE(TableDataCollection table, String columnNameWithRiboSeqReadsNumber, double riboSeqReadsThreshold, String columnNameWithStartCodonPositions, String columnNameWithTranscriptNames, AnalysisJobControl jobControl, int from, int to)
    {
        Map<String, List<Object[]>> result = new HashMap<>();
        String[] transcriptNames = TableUtils.readGivenColumnInStringTable(table, columnNameWithTranscriptNames);
        if( jobControl != null ) jobControl.setPreparedness(from + 2 * (to - from) / 10);
        double[] readsNumbers = TableUtils.readGivenColumnInDoubleTableAsArray(table, columnNameWithRiboSeqReadsNumber);
        if( jobControl != null ) jobControl.setPreparedness(from + 4 * (to - from) / 10);
        int[] startCodonPositions = TableUtils.readGivenColumnInIntegerTable(table, columnNameWithStartCodonPositions);
        if( jobControl != null ) jobControl.setPreparedness(from + 6 * (to - from) / 10);
        int newFrom = from + 6 * (to - from) / 10, difference = to - newFrom;
        for( int i = 0; i < transcriptNames.length; i++ )
        {
            if( jobControl != null ) jobControl.setPreparedness(newFrom + (i + 1) * difference / transcriptNames.length);
            if( readsNumbers[i] > riboSeqReadsThreshold )
            {
                Object[] array = new Object[]{startCodonPositions[i], readsNumbers[i]};
                result.computeIfAbsent(transcriptNames[i], key -> new ArrayList<>()).add(array);
            }
        }
        return result;
    }

    private List<GeneTranscript> getGeneTranscriptListInTableForTIE(DataElementPath pathToSequences, DataElementPath pathToTableWithRiboSeqData, String columnNameWithRiboSeqReadsNumber, double riboSeqReadsThreshold, String columnNameWithStartCodonPositions, String columnNameWithTranscriptNames, DataElementPath pathToTableWithMrnaSeqData, String columnNameWithMrnaSeqReadsNumber, double mrnaSeqReadsThreshold, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        int maximalDistance = 10;
        List<GeneTranscript> result = new ArrayList<>();
        TableDataCollection tableWithRiboSeqData = pathToTableWithRiboSeqData.getDataElement(TableDataCollection.class);
        Map<String, List<Object[]>> transcriptNamesAndStartCodonPositionsAndReadsNumbers = readTranscriptNamesAndObjectsInTableForTIE(tableWithRiboSeqData, columnNameWithRiboSeqReadsNumber, riboSeqReadsThreshold, columnNameWithStartCodonPositions, columnNameWithTranscriptNames, jobControl, from, from + (to - from) / 4);
        String[] transcriptNames = transcriptNamesAndStartCodonPositionsAndReadsNumbers.keySet().toArray(new String[0]);
        List<GeneTranscript> preliminaryListOfTranscripts = GeneTranscript.readTranscriptsInEnsembl(transcriptNames, pathToSequences, jobControl, from + (to - from) / 4, from + (to - from) / 2);
        
        Map<String, Double> transcriptNameAndMrnaSeqReads = TableUtils.readGivenColumnInDoubleTableAsMap(pathToTableWithMrnaSeqData, columnNameWithMrnaSeqReadsNumber);
        int newFrom = from + 3 * (to - from) / 4, difference = to - newFrom, iJobControl = 0, n = preliminaryListOfTranscripts.size();
        if( jobControl != null )
            jobControl.setPreparedness(newFrom);
        for( GeneTranscript gt : preliminaryListOfTranscripts )
        {
            if( jobControl != null )
                jobControl.setPreparedness(newFrom + ++iJobControl * difference / n);
            String transcriptName = gt.getTranscriptName();
            Interval cdsInterval = gt.getCdsInterval();
            if( cdsInterval == null ) continue;
            int startCodonPosition = cdsInterval.getFrom();
            List<Object[]> listOfArrays = transcriptNamesAndStartCodonPositionsAndReadsNumbers.get(transcriptName);
            Object[] selectedArray = null;
            int minimalDistance = Integer.MAX_VALUE;
            for( Object[] array : listOfArrays )
            {
                int distance = Math.abs((int)array[0] - startCodonPosition);
                if( distance <= maximalDistance && distance < minimalDistance )
                {
                    selectedArray = array;
                    minimalDistance = distance;
                }
            }
            if( selectedArray == null ) continue;
            Double mrnaSeqReads = transcriptNameAndMrnaSeqReads.get(transcriptName);
            if( mrnaSeqReads == null || mrnaSeqReads <= mrnaSeqReadsThreshold ) continue;
            Map<String, Double> map = new HashMap<>();
            map.put(FEATURE_30_RS_HARRINGTONINE, (Double)selectedArray[1]);
            map.put(FEATURE_31_RS_HARRINGTONINE_LG, Math.log10((double)selectedArray[1]));
            map.put(FEATURE_27_RS_MRNA, mrnaSeqReads);
            double mrnaSeqReadsNormalized = mrnaSeqReads / gt.getTranscriptLength();
            map.put(FEATURE_28_RS_MRNA_NORMALIZED, mrnaSeqReadsNormalized);
            map.put(FEATURE_32_TIE, (double)selectedArray[1] / mrnaSeqReadsNormalized);
            map.put(FEATURE_33_TIE_LG, Math.log10((double)selectedArray[1]) / mrnaSeqReadsNormalized);
            gt.setNameAndValueOfRiboSeqFeature(map);
            result.add(gt);
        }
        return result;
    }

    ////////////////////////////////0000//////////////////////////
    private void setValuesOfRibiSeqFeaturesForParticularGSE31539DataSet() throws Exception
    {
        TableDataCollection table = pathToData.getDataElement(TableDataCollection.class);
        for( int i = 0; i < geneTranscriptList.size(); i++ )
        {
            Map<String, Double> map = new HashMap<>();
            GeneTranscript gt = geneTranscriptList.get(i);
            String name = gt.getTranscriptName();
            Object[] rowElements = table.get(name).getValues();
            Double value = ((Number)rowElements[12]).doubleValue();
            map.put(FEATURE_6_TOTAL_TRLN_EFFICIENCY, value);
            value = ((Number)rowElements[10]).doubleValue();
            map.put(FEATURE_7_MEM_TRLN_EFFICIENCY, value);
            value = ((Number)rowElements[11]).doubleValue();
            map.put(FEATURE_8_CYT_TRLN_EFFICIENCY, value);
            value = ((Number)rowElements[7]).doubleValue();
            map.put(FEATURE_9_TOTAL_RIBO_RPKM, value);
            map.put(FEATURE_12_LG_TOTAL_RIBO_RPKM, Math.log10(1.0 + value));
            value = ((Number)rowElements[8]).doubleValue();
            map.put(FEATURE_10_MEM_RIBO_RPKM, value);
            map.put(FEATURE_13_LG_MEM_RIBO_RPKM, Math.log10(1.0 + value));
            value = ((Number)rowElements[8]).doubleValue();
            map.put(FEATURE_11_CYT_RIBO_RPKM, value);
            map.put(FEATURE_14_LG_CYT_RIBO_RPKM, Math.log10(1.0 + value));
            gt.setNameAndValueOfRiboSeqFeature(map);
        }
    }
    
    ////////////////////////////////0000//////////////////////////
    private void selectLincRnaTranscripts(AnalysisJobControl jobControl, int from, int to)
    {
        int difference = to - from;
        List<GeneTranscript> newGeneTranscriptList = new ArrayList<>();
        for( int i = 0; i < geneTranscriptList.size(); i++ )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / geneTranscriptList.size());
            GeneTranscript gt = geneTranscriptList.get(i);
            if( gt.getGeneType().equals(Gene.LINC_RNA) )
                newGeneTranscriptList.add(gt);
        }
        geneTranscriptList = newGeneTranscriptList;
    }
    
    ////////////////////////////////0000//////////////////////////
    private void getLincRNAswithHypotheticalCDS(DataElementPath pathToSequences, int orderOfStartCodon, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        int difference = to - from;
        List<GeneTranscript> newGeneTranscriptList = new ArrayList<>();
        for( int i = 0; i < geneTranscriptList.size(); i++ )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / geneTranscriptList.size());
            GeneTranscript gt = geneTranscriptList.get(i);
            byte[] sequence = gt.getTranscriptSequence(pathToSequences);
            if( ! gt.getGeneType().equals(Gene.LINC_RNA) || sequence == null || sequence.length < 6 ) continue;
            Interval interval = GeneTranscript.getCDSWithGivenOrderOfStartCodon(sequence, orderOfStartCodon);
            if( interval == null ) continue;
            gt.setCdsFromAndTo(interval);
            newGeneTranscriptList.add(gt);
        }
        geneTranscriptList = newGeneTranscriptList;
    }
    
    private void getRNAsWithHypotheticalCDSs(DataElementPath pathToSequences, int orderOfStartCodon, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        int difference = to - from;
        List<GeneTranscript> newGeneTranscriptList = new ArrayList<>();
        for( int i = 0; i < geneTranscriptList.size(); i++ )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / geneTranscriptList.size());
            GeneTranscript gt = geneTranscriptList.get(i);
            byte[] sequence = gt.getTranscriptSequence(pathToSequences);
            if( sequence == null || sequence.length < 6 ) continue;
            Interval interval = GeneTranscript.getCDSWithGivenOrderOfStartCodon(sequence, orderOfStartCodon);
            if( interval == null || gt.getCdsInterval().getFrom() == interval.getFrom() ) continue;
            gt.setCdsFromAndTo(interval);
            newGeneTranscriptList.add(gt);
        }
        geneTranscriptList = newGeneTranscriptList;
    }
    
    public Object[] getValuesOfRiboSeqFeature(String riboSeqFeatureName, String[] transcriptNames)
    {
        if( transcriptNames != null )
        {
            double[] riboSeqFeatureValues = MatrixUtils.getConstantVector(transcriptNames.length, Double.NaN);
            for( GeneTranscript gt : geneTranscriptList )
            {
                String transcriptName = gt.getTranscriptName();
                int index = ArrayUtils.indexOf(transcriptNames, transcriptName);
                if( index >= 0 )
                {
                    Map<String, Double> nameAndValueOfRiboSeqFeature = gt.getNameAndValueOfRiboSeqFeature();
                    if( nameAndValueOfRiboSeqFeature != null )
                    {
                        Double value = nameAndValueOfRiboSeqFeature.get(riboSeqFeatureName);
                        if( value != null )
                            riboSeqFeatureValues[index] = value;
                    }
                }
            }
            return new Object[]{riboSeqFeatureValues, transcriptNames};
        }
        String[] newTranscriptNames = new String[geneTranscriptList.size()];
        double[] riboSeqFeatureValues = MatrixUtils.getConstantVector(geneTranscriptList.size(), Double.NaN);
        for( int i = 0; i < geneTranscriptList.size(); i++ )
        {
            GeneTranscript gt = geneTranscriptList.get(i);
            newTranscriptNames[i] = gt.getTranscriptName();
            riboSeqFeatureValues[i] = gt.getNameAndValueOfRiboSeqFeature().get(riboSeqFeatureName);
        }
        return new Object[]{riboSeqFeatureValues, newTranscriptNames};
    }

    ////////////////////////////////0000//////////////////////////
    public Object[] getMrnaFeaturesDataMatrixAndTranscriptNames(String[] mrnaFeatureNames, Map<String, IPSSiteModel> nameAndSiteModel, boolean doExcludeMissingData, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        if( mrnaFeatureNames == null ) return null;
        int difference = to - from, iJobControl = 0;
        Map<String, double[]> map = new HashMap<>();
        for( GeneTranscript gt : geneTranscriptList )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + ++iJobControl * difference / geneTranscriptList.size());
            if( gt == null ) continue;
            double[] values = new double[mrnaFeatureNames.length];
            byte[] transcriptSequence = gt.getTranscriptSequence(pathToSequences);
            for(int j = 0; j < mrnaFeatureNames.length; j++ )
            {
                Double value = gt.getMrnaFeatureValue(mrnaFeatureNames[j], transcriptSequence, nameAndSiteModel);
                if( value != null )
                    values[j] = value;
                else if( ! doExcludeMissingData )
                    values[j] = Double.NaN;
                else
                {
                    values = null;
                    break;
                }
            }
            if( values != null  && transcriptSequence != null)
                map.put(gt.getTranscriptName(), values);
        }
        if( map.isEmpty() ) return null;
        double[][] dataMatrix = new double[map.size()][];
        String[] transcriptNames = new String[map.size()];
        int i = 0;
        for( Entry<String, double[]> entry : map.entrySet() )
        {
            transcriptNames[i] = entry.getKey();
            dataMatrix[i++] = entry.getValue();
        }
        return new Object[]{dataMatrix, transcriptNames};
    }
    
    ////////////////////////////////0000//////////////////////////
    public Object[] getMrnaAndRiboseqFeaturesDataMatrixAndFeatureNamesAndSequenceSample(String[] mrnaAndRiboseqFeatureNames, boolean doExcludeMissingData, boolean doCreateSequenceSample, boolean areFragmentsNearStartCodons, int leftBoundaryOfMrnaFragments, int fragmentLength, Map<String, IPSSiteModel> nameAndSiteModel, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        // 1. Create data matrix with mRNA features
        List<String> list = new ArrayList<>();
        for( String s : mrnaAndRiboseqFeatureNames )
            if( GeneTranscript.isMrnaFeatureName(s) )
                list.add(s);
        String[] newMrnaAndRiboseqFeatureNames = list.isEmpty() ? null : list.toArray(new String[0]);
        Object[] mrnaFeaturesDataMatrixAndTranscriptNames = getMrnaFeaturesDataMatrixAndTranscriptNames(newMrnaAndRiboseqFeatureNames, nameAndSiteModel, doExcludeMissingData, jobControl, from, from + 6 * (to - from) / 10);
        double[][] dataMatrix = null;
        String[] transcriptNames = null;
        if( mrnaFeaturesDataMatrixAndTranscriptNames != null )
        {
            dataMatrix = (double[][])mrnaFeaturesDataMatrixAndTranscriptNames[0];
            transcriptNames = (String[])mrnaFeaturesDataMatrixAndTranscriptNames[1];
        }
            
        // 2. Add Ribo-Seq features to data matrix
        Set<String> allRiboSeqFeatureNames = getAvailableRiboSeqFeatureNames(dataSetName).toSet();
        list = new ArrayList<>(Arrays.asList(mrnaAndRiboseqFeatureNames));
        list.retainAll(allRiboSeqFeatureNames);
        for( String riboseqFeatureName : list )
        {
            Object[] objects = getValuesOfRiboSeqFeature(riboseqFeatureName, transcriptNames);
            double[] values = (double[])objects[0];
            if( transcriptNames != null )
            {
                MatrixUtils.addColumnToMatrix(dataMatrix, values);
                newMrnaAndRiboseqFeatureNames = (String[])ArrayUtils.add(newMrnaAndRiboseqFeatureNames, newMrnaAndRiboseqFeatureNames.length, riboseqFeatureName);
            }
            else
            {
                newMrnaAndRiboseqFeatureNames = new String[]{riboseqFeatureName};
                transcriptNames = (String[])objects[1];
                dataMatrix = new double[transcriptNames.length][1];
                for( int i = 0; i < transcriptNames.length; i++ )
                    dataMatrix[i][0] = values[i];
            }
        }
        if( ! doCreateSequenceSample ) return new Object[]{dataMatrix, transcriptNames, newMrnaAndRiboseqFeatureNames, null};
        
        // 3. Create sequence sample
        Object[] objects = getSequenceSample(transcriptNames, areFragmentsNearStartCodons, leftBoundaryOfMrnaFragments, fragmentLength);
        String[] sequenceSample = (String[])objects[0];
        transcriptNames = (String[])objects[1];
        
        // 4. Final removing the missing data
        if( doExcludeMissingData )
        {
            List<String> newTranscriptNames = new ArrayList<>(), newSequenceSample = new ArrayList<>();
            List<double[]> newDataMatrix = new ArrayList<>();
            for( int i = 0; i < transcriptNames.length; i++ )
                if( sequenceSample[i] != null )
                {
                    if( dataMatrix == null )
                    {
                        newTranscriptNames.add(transcriptNames[i]);
                        newSequenceSample.add(sequenceSample[i]);
                    }
                    else if( ! MatrixUtils.doContainNaN(dataMatrix[i]) )
                    {
                        newTranscriptNames.add(transcriptNames[i]);
                        newSequenceSample.add(sequenceSample[i]);
                        newDataMatrix.add(dataMatrix[i]);
                    }
                }
            if( newTranscriptNames.isEmpty() ) return null;
            transcriptNames = newTranscriptNames.toArray(new String[0]);
            sequenceSample = newSequenceSample.toArray(new String[0]);
            if( dataMatrix != null )
                dataMatrix = newDataMatrix.toArray(new double[newDataMatrix.size()][]);
        }
        return new Object[]{dataMatrix, transcriptNames, newMrnaAndRiboseqFeatureNames, sequenceSample};
    }
    
    public static TableDataCollection writeDataMatrixAndSequenceSampleIntoTable(double[][] dataMatrix, String[] transcriptNames, String[] mrnaAndRiboseqFeatureNames, String[] sequenceSample, boolean areFragmentsNearStartCodons, int leftBoundaryOfMrnaFragments, int rightBoundaryOfMrnaFragments, DataElementPath pathToOutputs, String tableName)
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath(tableName));
        if( dataMatrix != null )
            for( String mrnaAndRiboseqFeatureName : mrnaAndRiboseqFeatureNames )
            {
                String s = mrnaAndRiboseqFeatureName.replace('/', '|');
                table.getColumnModel().addColumn(s, Double.class);
            }
        String s = EnsemblUtils.SEQUENCE_SAMPLE + ": near";
        s += areFragmentsNearStartCodons ? " start" : " stop";
        s += " codons [" + Integer.toString(leftBoundaryOfMrnaFragments) + ", " + Integer.toString(rightBoundaryOfMrnaFragments) + "]";
        if( sequenceSample != null )
            table.getColumnModel().addColumn(s, String.class);
        for( int i = 0; i < transcriptNames.length; i++ )
        {
            List<Object> row = new ArrayList<>();
            if( dataMatrix != null )
                for( int j = 0; j < dataMatrix[0].length; j++ )
                    row.add(dataMatrix[i][j]);
            if( sequenceSample != null )
                row.add(sequenceSample[i]);
            String name = transcriptNames[i].replace('/', '|');
            TableDataCollectionUtils.addRow(table, name, row.toArray(new Object[row.size()]), true);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return table;
    }
    
    private String[] getTranscriptNames()
    {
        String[] result = new String[geneTranscriptList.size()];
        for( int i = 0; i < geneTranscriptList.size(); i++ )
            result[i] = geneTranscriptList.get(i).getTranscriptName();
        return result;
    }
    
    private Object[] getSequenceSample(String[] transcriptNames, boolean areFragmentsNearStartCodons, int leftBoundaryOfMrnaFragments, int fragmentLength) throws Exception
    {
        String[] newTranscriptNames = transcriptNames != null ? transcriptNames : getTranscriptNames();
        String[] sequenceSample = new String[newTranscriptNames.length];
        for( int i = 0; i < sequenceSample.length; i++ )
            sequenceSample[i] = null;
        for( GeneTranscript gt : geneTranscriptList )
        {
            String transcriptName = gt.getTranscriptName();
            int index = ArrayUtils.indexOf(newTranscriptNames, transcriptName);
            if( index < 0 ) continue;
            int anchorPosition = areFragmentsNearStartCodons ? gt.getCdsInterval().getFrom() : gt.getCdsInterval().getTo();
            if( anchorPosition + leftBoundaryOfMrnaFragments < 0 || anchorPosition + leftBoundaryOfMrnaFragments + fragmentLength > gt.getTranscriptLength() ) continue;
            byte[] gtSequence = gt.getTranscriptSequence(pathToSequences);
            byte[] fragment = Olig.getSubByteArray(gtSequence, anchorPosition + leftBoundaryOfMrnaFragments, fragmentLength);
            sequenceSample[index] = new String(fragment);
        }
        return new Object[]{sequenceSample, newTranscriptNames};
    }

    /******************* Private stuff for INGOLIA_GSE30839_DATA_SET ********************************/
    /***
     * Read 2 tables 'tableS1' and 'tableS3' and create List<MrnaFeatures>;
     * 
     * Fragment of 'tableS1':
     * UCSC_ID  footprintsOnbase
     * uc009dtn.1  2.78
     * uc007mcc.1  0.00
     * uc007zqi.1  0.00
     * 
     * Fragment of 'tableS3':
     * ID   UCSC_ID  Gene    Init Codon [nt] Dist to CDS [codons]    Frame vs CDS    Init Context [-3 to +4] CDS Length [codons] Harr Peak Start Harr Peak Width harrReads   Peak Score  Codon   Product
     * 1    uc007afd.1  Mrpl15  248 79  1   AATATGG 15  247 2   368 2.61    aug internal-out-of-frame
     * 2    uc007afh.1  Lypla1  36  5   0   AACATGT 225 34  4   783 3.27    aug n-term-trunc
     * @throws Exception
     * ***/
    private List<GeneTranscript> getGeneTranscriptListFromParticularIngoliaGSE30839DataSet(DataElementPath pathToFolderWithIngoliaGSE30839DataSet,  Map<String, Double> transcriptsNamesAndRiboseqValuesForWholeTranscripts, Map<String, List<Integer>> transcriptNameAndIndecesOfRows, Map<String, double[]> transcriptNameAndRiboseqDensityAndMrnaDensity, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        List<GeneTranscript> result = new ArrayList<>();
        TableDataCollection table = pathToFolderWithIngoliaGSE30839DataSet.getChildPath("tableS3_2").getDataElement(TableDataCollection.class);
        int columnIndexForStartCodon = table.getColumnModel().optColumnIndex("Init Codon [nt]");
        int columnIndexForLengthCodon = table.getColumnModel().optColumnIndex("CDS Length [codons]");
        int columnIndexForHarrReads = table.getColumnModel().optColumnIndex("harrReads");
        int columnIndexForPeakScores = table.getColumnModel().optColumnIndex("Peak Score");
        if( columnIndexForStartCodon == -1 || columnIndexForLengthCodon == -1 || columnIndexForHarrReads == -1) return null;
        int difference = to - from, iJobControl = 0, n = transcriptNameAndIndecesOfRows.size();
        for( Entry<String, List<Integer>> entry : transcriptNameAndIndecesOfRows.entrySet() )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + ++iJobControl * difference / n);
            String transcriptName = entry.getKey();
            List<Integer> rowsInTableWithGivenTranscript = entry.getValue();
            byte[] transcriptSequence = EnsemblUtils.getSequenceFromFastaTrack(pathToSequences, transcriptName);
            if( transcriptSequence == null ) continue;
            for( int i = 0; i < rowsInTableWithGivenTranscript.size(); i++ )
            {
                int strartCodon = ((Number)table.getValueAt(rowsInTableWithGivenTranscript.get(i), columnIndexForStartCodon)).intValue();
                Map<String, Double> nameAndValueOfRiboSeqFeature = new HashMap<>();
                if( ! transcriptsNamesAndRiboseqValuesForWholeTranscripts.containsKey(transcriptName) ) continue;
                Double r2 = Math.max(0.01, transcriptsNamesAndRiboseqValuesForWholeTranscripts.get(transcriptName));
                Double r1 = ((Number)table.getValueAt(rowsInTableWithGivenTranscript.get(i), columnIndexForHarrReads)).doubleValue();
                Double score = ((Number)table.getValueAt(rowsInTableWithGivenTranscript.get(i), columnIndexForPeakScores)).doubleValue();
                nameAndValueOfRiboSeqFeature.put(FEATURE_1_R1, r1);
                nameAndValueOfRiboSeqFeature.put(FEATURE_2_R2, r2);
                nameAndValueOfRiboSeqFeature.put(FEATURE_3_RATIO_R1_R2, r1 / r2);
                nameAndValueOfRiboSeqFeature.put(FEATURE_4_LG_R1,  Math.log10(r1));
                nameAndValueOfRiboSeqFeature.put(FEATURE_5_RATIO_LG_R1_R2,  Math.log10(r1) / r2);
                nameAndValueOfRiboSeqFeature.put(FEATURE_15_PEAK_SCORE, score);
                double[] riboseqDensityAndMrnaDensity = transcriptNameAndRiboseqDensityAndMrnaDensity.get(transcriptName);
                if( riboseqDensityAndMrnaDensity != null )
                {
                    riboseqDensityAndMrnaDensity[0] = Math.max(1.0, riboseqDensityAndMrnaDensity[0]);
                    riboseqDensityAndMrnaDensity[1] = Math.max(1.0, riboseqDensityAndMrnaDensity[1]);
                    riboseqDensityAndMrnaDensity[2] = Math.max(0.01, riboseqDensityAndMrnaDensity[2]);
                    nameAndValueOfRiboSeqFeature.put(FEATURE_17_R3, riboseqDensityAndMrnaDensity[0]);
                    nameAndValueOfRiboSeqFeature.put(FEATURE_18_R4, riboseqDensityAndMrnaDensity[1]);
                    double translationEfficiency = riboseqDensityAndMrnaDensity[0] / riboseqDensityAndMrnaDensity[1];
                    nameAndValueOfRiboSeqFeature.put(FEATURE_19_TE, translationEfficiency);
                    nameAndValueOfRiboSeqFeature.put(FEATURE_20_LG_R3, Math.log10(riboseqDensityAndMrnaDensity[0]));
                    nameAndValueOfRiboSeqFeature.put(FEATURE_21_LG_R4, Math.log10(riboseqDensityAndMrnaDensity[1]));
                    nameAndValueOfRiboSeqFeature.put(FEATURE_22_LG_TE, Math.log10(translationEfficiency));
                    nameAndValueOfRiboSeqFeature.put(FEATURE_23_GIVEN_TE, riboseqDensityAndMrnaDensity[2]);
                    nameAndValueOfRiboSeqFeature.put(FEATURE_24_LG_GIVEN_TE, Math.log10(riboseqDensityAndMrnaDensity[2]));
                }
                Interval[] exonPositions = new Interval[]{new Interval(0, transcriptSequence.length - 1)};
                Interval cdsFromAndTo = GeneTranscript.getCDS(transcriptSequence, strartCodon);
                if( cdsFromAndTo == null ) continue;
                String stopCodonOlig = Olig.getStopCodon(cdsFromAndTo, transcriptSequence);
                if( stopCodonOlig == null ) continue;
                result.add(new GeneTranscript(transcriptName + ":" + i, null, null, null, Gene.PROTEIN_CODING, StrandType.STRAND_PLUS, cdsFromAndTo, exonPositions, nameAndValueOfRiboSeqFeature));
           }
       }
        return result;
    }
    
    private Map<String, double[]> readTranscriptNameAndRiboseqDensityAndMrnaDensityAndTEInTable(DataElementPath pathToFolderWithIngoliaGSE30839DataSet, String[] givenTranscriptNames, AnalysisJobControl jobControl, int from, int to)
    {
        Map<String, double[]> result = new HashMap<>();
        int difference = to - from;
        TableDataCollection table = pathToFolderWithIngoliaGSE30839DataSet.getChildPath("tableS1C_").getDataElement(TableDataCollection.class);
        String[] transcriptNames = table.names().collect( Collectors.toList() ).toArray( new String[0] );
        double[] riboseqDensities = TableUtils.readGivenColumnInDoubleTableAsArray(table, "ribosome");
        double[] mrnaDensities = TableUtils.readGivenColumnInDoubleTableAsArray(table, "mRNA");
        double[] givenTranslationEfficiencies = TableUtils.readGivenColumnInDoubleTableAsArray(table, "TE ratio");
        for( int i = 0; i < givenTranscriptNames.length; i++ )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / givenTranscriptNames.length);
            int index = ArrayUtils.indexOf(transcriptNames, givenTranscriptNames[i]);
            double[] riboseqDensityAndMrnaDensity = index < 0 ? null : new double[]{riboseqDensities[index], mrnaDensities[index], givenTranslationEfficiencies[index]};
            result.put(givenTranscriptNames[i], riboseqDensityAndMrnaDensity);
        }
        return result;
    }

    ///////////////////////////////////0000000000000000000//////////////////////////
    private static Map<String, List<Integer>> readMrnaNamesAndIndecesOfRows(DataElementPath pathToTable, String nameOfColumnWithMrnaName)
    {
        TableDataCollection table = pathToTable.getDataElement(TableDataCollection.class);
        Map<String, List<Integer>> result = new HashMap<>();
        int columnIndex = table.getColumnModel().optColumnIndex(nameOfColumnWithMrnaName);
        if( columnIndex == -1 ) return null;
        for( int rowIndex = 0; rowIndex < table.getSize(); rowIndex++ )
        {
            String mrnaName = (String)table.getValueAt(rowIndex, columnIndex);
            List<Integer> list = result.containsKey(mrnaName) ? result.get(mrnaName) : new ArrayList<>();
            list.add(rowIndex);
            result.put(mrnaName, list);
        }
        return result;
    }
    
    /******************* Private stuff for INGOLIA_GSE37744_(A,B,C) data sets ********************************/
    private void filtrationOfTranscriptNamesForIngolia_GSE37744(List<String> transcriptNames, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        int difference = to - from, iJobControl = 0, n = transcriptNames.size();
        TableDataCollection table = pathToData.getChildPath("knownGeneOld5").getDataElement(TableDataCollection.class);
        Iterator<String> it = transcriptNames.iterator();
        while( it.hasNext() )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + ++iJobControl * difference / n);
            RowDataElement row = table.get(it.next());
            if( row == null )
                it.remove();
            else
            {
                Object[] rowElements = row.getValues();
                String chromosome = (String)rowElements[0];
                if( chromosome.contains("_") || chromosome.equals("chrM") )
                    it.remove();
                else if( ((Number)rowElements[2]).intValue() == ((Number)rowElements[4]).intValue() || ((Number)rowElements[3]).intValue() == ((Number)rowElements[5]).intValue() )
                    it.remove();
            }
        }
    }
    
    private List<GeneTranscript> getGeneTranscriptListForIngoliaGSE37744DataSet(List<String> transcriptNames, Map<String, Double> transcriptsNamesAndRibiseqValues, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        List<GeneTranscript> result = new ArrayList<>();
        int difference = to - from, iJobControl = 0;
        TableDataCollection table = pathToData.getChildPath("knownGeneOld5").getDataElement(TableDataCollection.class);
        for( String transcriptName : transcriptNames )
        {
            Object[] rowElements = table.get(transcriptName).getValues();
            String chromosome = ((String)rowElements[0]).split(Pattern.quote("r"))[1];
            int strand = rowElements[1].equals("+") == true ? 2 : 3;
            int transcriptFrom = ((Number)rowElements[2]).intValue() + 1;
            int transcriptTo = ((Number)rowElements[3]).intValue();
            int exonsNumber = ((Number)rowElements[6]).intValue();
            String[] exonsFrom = ((String)rowElements[7]).split(Pattern.quote(","));
            String[] exonsTo = ((String)rowElements[8]).split(Pattern.quote(","));
            Interval[] exonPositions = new Interval[exonsNumber];
            for( int i = 0; i < exonsNumber; i++ )
                exonPositions[i] = strand == 2 ? new Interval(Integer.parseInt(exonsFrom[i]) + 1, Integer.parseInt(exonsTo[i])) : new Interval(Integer.parseInt(exonsFrom[exonsNumber - 1 - i]) + 1, Integer.parseInt(exonsTo[exonsNumber - 1 - i]));
            int chromosomalCdsFrom = ((Number)rowElements[4]).intValue() + 1;
            int chromosomalCdsTo = ((Number)rowElements[5]).intValue();
            int pos1 = 0, pos2 = 0, cumulativeLength = 0;
            for( Interval interval : exonPositions )
            {
                if( strand == 2 )
                {
                    if( interval.inside(chromosomalCdsFrom) )
                        pos1 = chromosomalCdsFrom - interval.getFrom() + cumulativeLength;
                    if( interval.inside(chromosomalCdsTo) )
                        pos2 = chromosomalCdsTo - interval.getFrom() + cumulativeLength;
                }
                else
                {
                    if( interval.inside(chromosomalCdsTo) )
                        pos1 = interval.getTo() - chromosomalCdsTo + cumulativeLength;
                    if( interval.inside(chromosomalCdsFrom) )
                        pos2 = interval.getTo() - chromosomalCdsFrom  + cumulativeLength;
                }
                cumulativeLength += interval.getLength();
            }
            Map<String, Double> nameAndValueOfRiboSeqFeature = new HashMap<>();
            double value = transcriptsNamesAndRibiseqValues.get(transcriptName);
            nameAndValueOfRiboSeqFeature.put(FEATURE_1_R1_SHORT, value);
            nameAndValueOfRiboSeqFeature.put(FEATURE_4_LG_Rs, Math.log10(1.0 + value));
            if( pos2 - pos1 + 1 > 9 )
            result.add(new GeneTranscript(transcriptName, chromosome, new Interval(transcriptFrom, transcriptTo), null, null, strand, new Interval(pos1, pos2), exonPositions, nameAndValueOfRiboSeqFeature));
            if( jobControl != null )
                jobControl.setPreparedness(from + ++iJobControl * difference / transcriptNames.size());
        }
        return result;
    }

    //temp
    //private static Logger log = Logger.getLogger(ParticularRiboSeq.class.getName());
}

//////////////////////////////// for testing ////////////////////
// temporary for testing
/***
public void test(List<String> trNames, Logger log, DataElementPath pathToSequences)
{
    Map<String, Sequence> chromosomeAndSequence = EnsemblUtils.getChromosomeAndSequence(pathToSequences);
    for( GeneTranscript gt : geneTranscriptList )
    {
        String name = gt.getTranscriptName();
        if( ! trNames.contains(name) ) continue;
        Interval inter = gt.getInterval();
        Interval cdsInter = gt.getCdsInterval();
        Interval[] exPoss = gt.getExonPositions();
        log.info(" Name = " + name + " strand = " + gt.getStrand() + " chr = " + gt.getChromosome());
        log.info("interval = " + inter.getFrom() + " " + inter.getTo() + " length = " + inter.getLength());
        log.info("cds interval = " + cdsInter.getFrom() + " " + cdsInter.getTo());
        for( int j = 0; j < exPoss.length; j++ )
            log.info("exon " + j + " : " + exPoss[j].getFrom() + " " + exPoss[j].getTo());
        Sequence fullChromosome = chromosomeAndSequence.get(gt.getChromosome());
        byte[] mrna = gt.getSequence(fullChromosome);
        log.info(" seq = " + new String(mrna));
    }
}
***/

/***
 *     private void test(Track track)
    {
        for( GeneTranscript gt : geneTranscriptList )
        {
            String name = gt.getTranscriptName();
            Interval inter = gt.getInterval();
            Interval cdsInter = gt.getCdsInterval();
            Interval[] exPoss = gt.getExonPositions();
            String chr = gt.getChromosome();
            int strand = gt.getStrand();
            int cdsStartPos = gt.getChromosomalCDSstartPosition();
            Interval interval = strand == 2 ? new Interval(cdsStartPos, cdsStartPos + 10) : new Interval(cdsStartPos - 10, cdsStartPos);
            Sequence fullChromosome = pathToSequences.getChildPath(chr).getDataElement(AnnotatedSequence.class).getSequence();
            Sequence sequence = new SequenceRegion(fullChromosome, interval, false, false);
            if( strand == 3 )
                sequence = SequenceRegion.getReversedSequence(sequence);
            byte[] seq = sequence.getBytes();
            Olig.toLowerCase(seq);
            boolean isAUG = Olig.isGivenOlig(seq, 0, GeneTranscript.ATG);
            if( ! isAUG )
            {
                log.info(" Name = " + name + " strand = " + strand + " chr = " + chr);
                log.info("chromosomal CDS start position  = " + cdsStartPos);
                log.info("interval = " + inter.getFrom() + " " + inter.getTo() + " length = " + inter.getLength());
                log.info("cds interval = " + cdsInter.getFrom() + " " + cdsInter.getTo());
                for( int j = 0; j < exPoss.length; j++ )
                    log.info("exon " + j + " : " + exPoss[j].getFrom() + " " + exPoss[j].getTo());
                log.info(" start cds sequence  = " + sequence);
            }
        }
    }
***/

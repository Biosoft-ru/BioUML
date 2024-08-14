package biouml.plugins.bindingregions.mrnaanalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.mrnautils.GeneTranscript;
import biouml.plugins.bindingregions.mrnautils.ParticularRiboSeq;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.EnsemblUtils.Gene;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.Olig;
import biouml.plugins.bindingregions.utils.SequenceSampleUtils;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;
import biouml.plugins.bindingregions.utils.SiteModelsComparisonUtils;
import biouml.plugins.bindingregions.utils.SitePrediction;
import biouml.plugins.bindingregions.utils.StatUtil.TestsForExponentiality;
import biouml.plugins.bindingregions.utils.TableUtils;
import biouml.plugins.bindingregions.utils.TrackInfo;
import biouml.standard.type.Species;
import gnu.trove.decorator.TObjectIntMapDecorator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.bsa.analysis.WeightMatrixModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

// private static Logger log = Logger.getLogger(StatUtil.class.getName());
/**
 * @author yura
 * Currently this analysis is temporary. Now it contains some temporary stuff for answering the questions from Kel
 */
public class LincRNAandMrnaFeatures extends AnalysisMethodSupport<LincRNAandMrnaFeatures.LincRNAandMrnaFeaturesParameters>
{
    private static byte[] ATG = new byte[]{'a', 't', 'g'};
    private static byte[][] STOP_CODONS = new byte[][]{{'t', 'a', 'g'}, {'t', 'a', 'a'}, {'t', 'g', 'a'}};

    public LincRNAandMrnaFeatures(DataCollection<?> origin, String name)
    {
        super(origin, name, new LincRNAandMrnaFeaturesParameters());
    }
    
    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info("Analysis of lincRNA: under construction");
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        Species givenSpecie = parameters.getSpecies();
        DataElementPath pathToInputTable = parameters.getPathToTableWithCoeffficients();
        DataElementPath pathToFile = parameters.getPathToFile();
        DataElementPath pathToOutputs = parameters.getOutputPath();
    
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        // 1. For Kel
        // getMeanEntropiesForGivenMatrices(pathToOutputs);
        // getInformationOnGTRDtracks(pathToOutputs);
        
        // 2. Temporary for TIE prediction
        // DataElementPath pathToTableWithRegressionCoeffficients = parameters.getPathToTableWithCoeffficients();
        // Object[] objects = getBestSitesForSequenceSample(pathToTableWithRegressionCoeffficients);
        // TableUtils.writeDoubleTable((double[][])objects[2], (String[]) objects[0], (String[])objects[1], pathToOutputs, "maxScores");
        
        // 3. Start codon predictions in given transcripts by 4 matrices
        // predictStartCodonsForGivenTranscripts(pathToSequences, pathToOutputs);
        
        // 4. Normalization of TE from different datasets
        // normalizationOfTeFromDifferentDatasets(pathToTableWithRegressionCoeffficients, pathToOutputs);
        
        // 5. Calculation of number of AUG in liders of protein coding transcripts
        // treatmentOfAUGinLidersOfProteinCodingTranscripts( pathToSequences, pathToOutputs);
        
        // 6. New article 2
        // predictStartCodonsAfterExponentialityTest2(pathToSequences, pathToInputTable, pathToOutputs);
        // predictStartCodonsAfterExponentialityTest(pathToSequences, pathToInputTable, pathToOutputs);
        // treatmentOfCdsFrames(pathToSequences, pathToOutputs);
        // calculateAndWriteMatricesScores(pathToOutputs);
        // treatmentOfCds(pathToSequences);
                
        // 7. FANTOM5
//        getDataMatrixFromFile(pathToFile, pathToOutputs);
//        writePeakCoordinatesFromFileToTable(pathToFile, pathToOutputs);
        
        // 8. GTRD <-> Fantom5 analysis
        // 8.1.
//        int thresholdForNumberOfSites = 500, thresholdForNumberOfTracks = 1;
//        DataElementPath pathToInputTracks = DataElementPath.create("databases/GTRD2/Data/peaks/macs");
//        DataElementPath pathToTableWithCellLineSynonyms = DataElementPath.create("data/Collaboration/yura_test/Data/GTRD_analysis_2/Human_Build37/Tables/AuxiliaryTables/cellLineSynonyms_");
//        coverageOfTssByPeacksInCellLines(pathToSequences, pathToInputTracks, pathToTableWithCellLineSynonyms, pathToInputTable, pathToOutputs, givenSpecie, thresholdForNumberOfSites, thresholdForNumberOfTracks, log);
//        return pathToOutputs.getDataCollection();
        
        // 8.2.        
//        int thresholdForNumberOfTracks = 10;
//        DataElementPath pathToInputCoverageDataMatrices = DataElementPath.create("data/Collaboration/yura_test/Data/_YURA4/FANTOM5/ANSWER_1/ANS05_coverage_matrices_hg19_macs");
//        calculateCellLineFrequenciesFromCoverageDataMatrix(pathToInputCoverageDataMatrices, pathToOutputs,  thresholdForNumberOfTracks, log);
        
        // 8.3.
        int thresholdForNumberOfTracks = 1;
        DataElementPath pathToInputCoverageDataMatrices = DataElementPath.create("data/Collaboration/yura_test/Data/_YURA4/FANTOM5/ANSWER_1/ANS05_coverage_matrices_hg19_macs");
        calculateCellLinePattrensFromCoverageDataMatrix(pathToInputCoverageDataMatrices, pathToOutputs,  thresholdForNumberOfTracks, log);
        return pathToOutputs.getDataCollection();
    }

    /************** GTRD <-> Fantom5 ***********************/
    
    public static void calculateCellLinePattrensFromCoverageDataMatrix(DataElementPath pathToInputCoverageDataMatrices, DataElementPath pathToOutputs,  int thresholdForNumberOfTracks, Logger log)
    {
        // 1. Create the list of table names.
        List<String> tableNames = createTableNameList(pathToInputCoverageDataMatrices, thresholdForNumberOfTracks);
        
        for( String tableName : tableNames ) 
        {
            // 2. Calculation of frequencies of patterns.
            TObjectIntMap<String> patternCounts = new TObjectIntHashMap<>();
            String[] columnNames = TableUtils.getColumnNamesInTable(pathToInputCoverageDataMatrices.getChildPath(tableName).optDataElement(TableDataCollection.class));
            String[][] dataMatrix = (String[][])TableUtils.readStringDataSubMatrix(pathToInputCoverageDataMatrices.getChildPath(tableName), columnNames)[1];
            log.info("tableName = " + tableName + " number of columns = " + columnNames.length);
            for( int i = 0; i < dataMatrix.length; i++ )
            {
                String pattern = "";
                for( int j = 0; j < columnNames.length; j++ )
                    pattern += dataMatrix[i][j];
                patternCounts.adjustOrPutValue(pattern, 1, 1);
            }
            // 3. Write results in to table.
            Map<String, Integer> patternAndCount = new TObjectIntMapDecorator<>(patternCounts);
            log.info("number of patterns = " + patternAndCount.size());
            String[] rowNames = new String[patternAndCount.size()];
            double[] frequencies = new double[patternAndCount.size()];
            String nameOfColumn = "Frequencies of patterns in ";
            for( String s : columnNames )
                nameOfColumn += "_" + s;
            
/////////////////// temp
            log.info("columnNames.length = " + columnNames.length);
            log.info("nameOfColumn = " + nameOfColumn);
            
            int i = 0;
            for( Entry<String, Integer> entry : patternAndCount.entrySet() )
            {
                rowNames[i] = entry.getKey();
                frequencies[i] = (double)entry.getValue() / (double)dataMatrix.length;
                i++;
            }
            TableUtils.writeDoubleTable(frequencies, rowNames, nameOfColumn, pathToOutputs, tableName);
        }
    }

    public static TableDataCollection calculateCellLineFrequenciesFromCoverageDataMatrix(DataElementPath pathToInputCoverageDataMatrices, DataElementPath pathToOutputs,  int thresholdForNumberOfTracks, Logger log)
    {
        // 1. Create the list of table names.
        List<String> tableNames = createTableNameList(pathToInputCoverageDataMatrices, thresholdForNumberOfTracks);
        
        // 2. Calculation of frequency data matrix.
        List<String> rowNames = TableUtils.readRowNamesInTable(pathToInputCoverageDataMatrices.getChildPath(tableNames.get(0)));
        int n = rowNames.size(), m = tableNames.size();
        log.info("output data matrix dimension, n x m = " + n + " x " + m);
        double[][] frequencies = new double[n][m];
        for( int j = 0; j < tableNames.size(); j++ ) 
        {
            String[] names = TableUtils.getColumnNamesInTable(pathToInputCoverageDataMatrices.getChildPath(tableNames.get(j)).optDataElement(TableDataCollection.class));
            String[][] dataMatrix = (String[][])TableUtils.readStringDataSubMatrix(pathToInputCoverageDataMatrices.getChildPath(tableNames.get(j)), names)[1];
            log.info("tableName = " + tableNames.get(j) + " number of columns = " + names.length);
            for( int i = 0; i < n; i++ )
            {
                int sum = 0;
                for( int jj = 0; jj < names.length; jj++ )
                    if( dataMatrix[i][jj] == "+" )
                        sum += 1;
                frequencies[i][j] = (double)sum / (double)names.length;
            }
        }
        
        // 3. Write data matrix.
        return TableUtils.writeDoubleTable(frequencies, rowNames.toArray(new String[0]), tableNames.toArray(new String[0]), pathToOutputs, "frequencies_in_cell_lines_" +Integer.toString(thresholdForNumberOfTracks));
    }
    
    private static List<String> createTableNameList(DataElementPath pathToTables, int thresholdForNumberColumns)
    {
        DataCollection<DataElement> tables = pathToTables.getDataCollection(DataElement.class);
        List<String> tableNames = tables.getNameList();
        for( int i = tableNames.size() - 1; i >= 0; i-- )
        {
            TableDataCollection table = pathToTables.getChildPath(tableNames.get(i)).optDataElement(TableDataCollection.class);
            if( table == null )
                tableNames.remove(i);
            else if( TableUtils.getColumnNamesInTable(table).length < thresholdForNumberColumns )
                tableNames.remove(i);
        }
        return tableNames;
    }
   
    public static void coverageOfTssByPeacksInCellLines(DataElementPath pathToSequences, DataElementPath pathToInputTracks, DataElementPath pathToTableWithCellLineSynonyms, DataElementPath pathToTableWithTss, DataElementPath pathToOutputs, Species givenSpecie, int thresholdForNumberOfSites, int thresholdForNumberOfTracks, Logger log)
    {
        Map<String, List<TrackInfo>> cellLineAndTrackInfos = getCellLineAndTrackInfos(pathToInputTracks, givenSpecie, pathToTableWithCellLineSynonyms, thresholdForNumberOfSites, thresholdForNumberOfTracks);
        log.info("1. Number of selected cell lines = " + cellLineAndTrackInfos.size());
        
        // Remove treated cell lines
        DataCollection<DataElement> tables = pathToOutputs.getDataCollection(DataElement.class);
        List<String> tableNames = tables.getNameList();
        for( String tableName : tableNames )
            cellLineAndTrackInfos.remove(tableName);
        log.info("2. Number of remained (non-treated) cell lines = " + cellLineAndTrackInfos.size());
        
        Object[] objects = readTSSInTable(pathToTableWithTss);
        String[] peakNames = (String[])objects[0];
        log.info("3. Number of TSS peaks = " + peakNames.length);
        
        
        // temporary
//        for( int i = 0; i < peakNames.length; i++ )
//            log.info("peakNames = " + ((String[])objects[0])[i] + " chromosome = " + ((String[])objects[1])[i] +
//                     " peak starts = " + ((int[])objects[2])[i] + " peak ends = " + ((int[])objects[3])[i]);

            
        
        for( Entry<String, List<TrackInfo>> entry : cellLineAndTrackInfos.entrySet() )
        {
            String cellLine = entry.getKey();
            List<TrackInfo> trackInfos = entry.getValue();
            String[] trackNames = new String[trackInfos.size()];
            log.info("cell line = " + cellLine + " number of tracks = " + trackNames.length);
            for( int i = 0; i < trackNames.length; i++ )
                trackNames[i] = trackInfos.get(i).getTrackName();
            String[][] dataMatrix = calculateCoverageDataMatrix(pathToSequences, pathToInputTracks, trackNames, peakNames, (String[])objects[1], (int[])objects[2], (int[])objects[3], log);
            TableUtils.writeStringTable(dataMatrix, peakNames, trackNames, pathToOutputs.getChildPath(cellLine));
        }
    }

    private static String[][] calculateCoverageDataMatrix(DataElementPath pathToSequences, DataElementPath pathToInputTracks, String[] trackNames, String[] peakNames, String[] chromosomeNames, int[] peakStarts, int[] peakEnds, Logger log)
    {
        // 1. Initialization of data matrix.
        int n = peakNames.length, m = trackNames.length;
        String[][] dataMatrix = new String[n][m];
        for( int i = 0; i < n; i++ )
            for( int j = 0; j < m; j ++ )
                dataMatrix[i][j] = "-";
        
        // 2. Calculation of data matrix.
        for ( int j = 0; j < m; j++ )
        {
            log.info("   j = " + j + " m = " + m + " trackName = " + trackNames[j]);
            Track track = pathToInputTracks.getChildPath(trackNames[j]).getDataElement(Track.class);
            for( int i = 0; i < n; i++ )
            {
                String sequence = pathToSequences.getChildPath(chromosomeNames[i]).toString();
                if( track.getSites(sequence, peakStarts[i], peakEnds[i]).getSize() > 0 )
                    dataMatrix[i][j] = "+";
            }
        }
        return dataMatrix;
    }
    
    public static Map<String, List<TrackInfo>> getCellLineAndTrackInfos(DataElementPath pathToInputTracks, Species givenSpecie, DataElementPath pathToTableWithCellLineSynonyms, int thresholdForNumberOfSites, int thresholdForNumberOfTracks)
    {
        List<TrackInfo> trackInfos = TrackInfo.getTracksInfo(pathToInputTracks, givenSpecie, null, null);
        TrackInfo.changeSynonymCellLines(trackInfos, pathToTableWithCellLineSynonyms, MessageBundle.CELL_LINE_COLUMN, MessageBundle.CELL_LINE_SYNONYM_COLUMN);
        trackInfos = TrackInfo.removeSmallTracks(trackInfos, thresholdForNumberOfSites);
        Map<String, List<TrackInfo>> result = new HashMap<>();
        for( TrackInfo ti : trackInfos )
            result.computeIfAbsent(ti.getCellLine(), key -> new ArrayList<>()).add(ti);
        removeRareCellLines(result, thresholdForNumberOfTracks); 
        return result;
    }
    
    private static Object[] readTSSInTable(DataElementPath pathToTableWithTss)
    {
        TableDataCollection table = pathToTableWithTss.optDataElement(TableDataCollection.class);
        String[] chromosomeNames = TableUtils.readGivenColumnInStringTable(table, "chromosome");
        int[] peakStarts = TableUtils.readGivenColumnInIntegerTable(table, "TSS_start");
        int[] peakEnds = TableUtils.readGivenColumnInIntegerTable(table, "TSS_start_+1");
        String[] peakNames = TableUtils.readRowNamesInTable(table);
        return new Object[]{peakNames, chromosomeNames, peakStarts, peakEnds};
    }
    
    
    public static void removeRareCellLines(Map<String, List<TrackInfo>> cellLinesAndTrackInfos, int thresholdForNumberOfTracks)
    {
        for( String cellLine : cellLinesAndTrackInfos.keySet() )
            if( cellLinesAndTrackInfos.get(cellLine).size() < thresholdForNumberOfTracks )
                cellLinesAndTrackInfos.remove(cellLine);
    }
    /*************************************/

    
    ///////////// Fantom5 : import data matrix by transforming File to TableDataCollection

    private void writePeakCoordinatesFromFileToTable(DataElementPath pathToFile, DataElementPath pathToOutputs) throws IOException
    {
        // 1. Table initialization.
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath(pathToFile.getName() + '_'));
        table.getColumnModel().addColumn("chromosome", String.class);
        table.getColumnModel().addColumn("strand", String.class);
        table.getColumnModel().addColumn("CAGE_peak_start", Integer.class);
        table.getColumnModel().addColumn("CAGE_peak_end", Integer.class);
        table.getColumnModel().addColumn("TSS_start", Integer.class);
        table.getColumnModel().addColumn("TSS_start_+1", Integer.class);
        table.getColumnModel().addColumn("score", Integer.class);
        table.getColumnModel().addColumn("CAGE_peak_name_in_FANTOM", String.class);

        // 2. Read lines in text file and add the corresponding row into table  
        File propertiesFile = pathToFile.getDataElement(FileDataElement.class).getFile();
        try( BufferedReader reader = ApplicationUtils.asciiReader(propertiesFile) )
        {
            int index = 0;
            while( reader.ready() )
            {
                index += 1;
                String line = reader.readLine();
                if( line == null ) break;
                String[] tokens = TextUtil.split(line, '\t');
                if( tokens.length < 8 || tokens[0].length() < 4 ) continue;
                String chromosome = tokens[0].substring(3);
                Object[] row = new Object[]{chromosome, tokens[5], tokens[1], tokens[2], tokens[6],
                                            tokens[7], Integer.parseInt(tokens[4]), tokens[3]};
                TableDataCollectionUtils.addRow(table, "F_" + Integer.toString(index), row, true);
            }
            reader.close();
            table.finalizeAddition();
            CollectionFactoryUtils.save(table);
        }
    }

    private void getDataMatrixFromFile(DataElementPath pathToFile, DataElementPath pathToOutputs) throws IOException
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath(pathToFile.getName() + '_'));
        File propertiesFile = pathToFile.getDataElement(FileDataElement.class).getFile();
        int nToRemove = 0, nToAccept = 0;
        String[] cellNames = null, newCellNames = null;
        try( BufferedReader reader = ApplicationUtils.asciiReader(propertiesFile) )
        {
            int index = 0;
            while( reader.ready() )
            {
                index += 1;
                String line = reader.readLine();
                if( line == null ) break;
                String[] tokens = TextUtil.split(line, '\t');
                if( tokens.length < 3 || tokens[0].length() < 3 ) continue;
                if( tokens[0].charAt(0) == '#' && tokens[0].charAt(1) == '#' ) continue;
                if( tokens[0].equals("01STAT:MAPPED") ) continue;
                switch( tokens[0] )
                {
                    case "gene"         : nToAccept = tokens.length - 1; break;
                    case "00Annotation" : nToRemove = tokens[0].equals("short_description") ? 5 : 0;
                                          nToAccept = tokens[0].equals("short_description") ? tokens.length - 6 : tokens.length - 1; break;
                    default             : if( tokens.length != 1 + nToRemove + nToAccept || nToAccept == 0 ) break;
                                          Object[] row = new Object[nToAccept];
                                          for( int i = 0; i < nToAccept; i++ )
                                              row[i] = tokens[1 + nToRemove + i].equals("NA") ? Double.NaN : Double.parseDouble(tokens[1 + nToRemove + i]);
                                          TableDataCollectionUtils.addRow(table, tokens[0], row, true);
                                          log.info("index = " + index + " tokens[0] = " + tokens[0]);
                }
                
                // Identification of cellNames and add current file row into table 
                if( tokens[0].equals("gene") || tokens[0].equals("00Annotation") )
                {
                    cellNames = new String[nToAccept];
                    for( int i = 0; i < nToAccept; i++ )
                        cellNames[i] = tokens[1 + nToRemove + i];
                    newCellNames = new String[nToAccept];
                    for( int i = 0; i < nToAccept; i++ )
                    {
                        newCellNames[i] = "Cell_" + Integer.toString(i + 1);
                        table.getColumnModel().addColumn(newCellNames[i], Double.class);
                    }
                }
            }
            reader.close();
            table.finalizeAddition();
            CollectionFactoryUtils.save(table);
        }
        TableUtils.writeStringTable(cellNames, newCellNames, "cell_names_in_FANTOM5", pathToOutputs.getChildPath(pathToFile.getName() + "_cell_names"));
    }
    
    ///////////////////// New for Volkova article 2 ////////////////////////////

    // Consider exponentiality of frames for high start codon scores
    private void predictStartCodonsAfterExponentialityTest2(DataElementPath pathToSequences, DataElementPath pathToInputTable, DataElementPath pathToOutputs) throws Exception
    {
        int window = 100;
        double maxScoreThreshold = 4.0;
        // 1. Read data matrix with exponentiality tests.
        TableDataCollection table = pathToInputTable.getDataElement(TableDataCollection.class);
        String[] columnNames = new String[]{"Cox_and_Oakes_p_value_frame_1", "Cox_and_Oakes_p_value_frame_2", "Cox_and_Oakes_p_value_frame_3",
                                            "Gini_p_value_frame_1", "Gini_p_value_frame_2", "Gini_p_value_frame_3"};
        String columnNameWithTranscriptTypes = "transcript_type";
        Object[] objects = TableUtils.readDataSubMatrix(table, columnNames);
        String[] transcriptNames = (String[])objects[0];
        double[][] dataMatrix = (double[][])objects[1];
        String[] transcriptTypes = TableUtils.readGivenColumnInStringTable(table, columnNameWithTranscriptTypes);
        
        // 2. Create site models
        String[] pathsToMatrices = new String[]
                {"data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices_revised/near_start_codon_4_matrices_IPS_threshold_3.5/startCodon_1_iteration_15_revised",
                 "data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices_revised/near_start_codon_4_matrices_IPS_threshold_3.5/startCodon_2_iteration_15_revised",
                 "data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices_revised/near_start_codon_4_matrices_IPS_threshold_3.5/startCodon_3_iteration_15_revised"}; 
        List<IPSSiteModel> siteModelsList = new ArrayList<>();
        Alphabet alphabet = null;
        for( String pathToMatrix : pathsToMatrices)
        {
            FrequencyMatrix frequencyMatrix = DataElementPath.create(pathToMatrix).getDataElement(FrequencyMatrix.class);
            alphabet = frequencyMatrix.getAlphabet();
            siteModelsList.add((IPSSiteModel)SiteModelsComparison.getSiteModel(SiteModelsComparison.IPS_SITE_MODEL, frequencyMatrix, 0.01, window - 3));
        }
        IPSSiteModel[] siteModels = siteModelsList.toArray(new IPSSiteModel[0]);
        log.info("1. Creation of siteModels: n = " + siteModels.length);
        
        // 3. Select values for transcriptNameList, pValues, frameIndeces
        List<String> transcriptNameList = new ArrayList<>();
        for( int i = 0; i < dataMatrix.length; i++ )
        {
            log.info(" i = " + i + " transcriptTypes = " + transcriptTypes[i] + " dataMatrix[i][3] = " + dataMatrix[i][3]);
            if( ! transcriptTypes[i].equals("lncRNA") ) continue;
            for(int j = 0; j < 6; j++ )
                if( Double.isNaN(dataMatrix[i][j]) ) continue;
            transcriptNameList.add(transcriptNames[i]);
        }
        List<GeneTranscript> geneTranscriptList = GeneTranscript.readTranscriptsInEnsembl(transcriptNameList.toArray(new String[0]), pathToSequences, jobControl, 0, 40);
        log.info("2. Number of pre-selected transcripts, nn = " + transcriptNameList.size());

        
        
        // 4. Create output table.
        table = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath("strong_start_Codons_in_lncRNAs"));
        table.getColumnModel().addColumn("transcript_name", String.class);
        table.getColumnModel().addColumn("transcript_length", Double.class);
        table.getColumnModel().addColumn("pvalue_for_exponentiality", Double.class);
        table.getColumnModel().addColumn("cds_length", Double.class);
        table.getColumnModel().addColumn("cds_frame", Double.class);
        table.getColumnModel().addColumn("MAT1", Double.class);
        table.getColumnModel().addColumn("MAT2", Double.class);
        table.getColumnModel().addColumn("MAT3", Double.class);
        table.getColumnModel().addColumn("max_score", Double.class);

        //
        int index = 0;
        for( int i = 0; i < geneTranscriptList.size(); i++ )
        {
            GeneTranscript gt = geneTranscriptList.get(i);
            double transcriptLength = gt.getTranscriptLength();
            String transcriptName = transcriptNameList.get(i);
            
            // int frameIndex = frameIndeces.get(i);
            // double pvalueForExponentiality = pValues.get(i);
            byte[] seqence = gt.getTranscriptSequence(pathToSequences);

            ////////////////////////
            log.info("i = " + i + " Name = " + transcriptName + " Length = " + transcriptLength);

            for( int j = 0; j < seqence.length - 2; j ++ )
            {
                if( Olig.isGivenOlig(seqence, j, ATG) == false ) continue;
                if( j - window / 2 < 0 || j + window / 2 >= seqence.length - 1 ) continue;
                Interval cdsInterval = GeneTranscript.getCDS(seqence, j);
                if( cdsInterval == null ) continue;
                int frameIndex = j - (j / 3) * 3;
                double pvalueForExponentiality = Math.min(dataMatrix[i][frameIndex], dataMatrix[i][3 + frameIndex]);
                double cdsLength = (double)cdsInterval.getLength(); 
                byte[] seq = Olig.getSubByteArray(seqence, j - window / 2, window);
                Sequence sequence = new LinearSequence("aaa", seq, alphabet);
                double score1 = SitePrediction.findBestSite(siteModels[0], sequence, false).getScore();
                double score2 = SitePrediction.findBestSite(siteModels[1], sequence, false).getScore();
                double score3 = SitePrediction.findBestSite(siteModels[2], sequence, false).getScore();
                double maxScore = Math.max(score3, Math.max(score1, score2));
                if( maxScore < maxScoreThreshold ) continue;
                Object[] row = new Object[]{transcriptName, transcriptLength,pvalueForExponentiality,
                                            cdsLength, (double)frameIndex, score1, score2, score3, maxScore};
                TableDataCollectionUtils.addRow(table, Integer.toString(index), row, true);
                index += 1;
            }
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }
    
    // The list of significant ORF predictions in lncRNAs is created (for answer to reviewers)
    private void predictionOfCDSsInLncRNAs(DataElementPath pathToSequences, DataElementPath pathToOutputs) throws Exception
    {
        double pValueThreshold = 0.05, maxScoreThreshold = 4.0;
        int window = 100;

        // 1. Create list of lncRnaTranscripts.
        List<GeneTranscript> geneTranscripts = GeneTranscript.readTranscriptsInEnsembl(null, pathToSequences, null, 0, 0);
        log.info("size of geneTranscripts = " + geneTranscripts.size());
        List<GeneTranscript> lncRnaTranscripts = new ArrayList<>();
        for( GeneTranscript gt : geneTranscripts )
            if( gt.getGeneType().equals(Gene.LINC_RNA))
                lncRnaTranscripts.add(gt);
        log.info("size of lncRnaTranscripts = " + lncRnaTranscripts.size());
        
        // 2. Create site models.
        String[] pathsToMatrices = new String[]
                {"data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices_revised/near_start_codon_4_matrices_IPS_threshold_3.5/startCodon_1_iteration_15_revised",
                 "data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices_revised/near_start_codon_4_matrices_IPS_threshold_3.5/startCodon_2_iteration_15_revised",
                 "data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices_revised/near_start_codon_4_matrices_IPS_threshold_3.5/startCodon_3_iteration_15_revised"}; 
        Alphabet alphabet = null;
        IPSSiteModel[] siteModels = new IPSSiteModel[0];
        for( int i = 0; i < pathsToMatrices.length; i++ )
        {
            FrequencyMatrix frequencyMatrix = DataElementPath.create(pathsToMatrices[i]).getDataElement(FrequencyMatrix.class);
            alphabet = frequencyMatrix.getAlphabet();
            siteModels[i] = ((IPSSiteModel)SiteModelsComparison.getSiteModel(SiteModelsComparison.IPS_SITE_MODEL, frequencyMatrix, 0.01, window - 3));
        }
        log.info("Creation of siteModels: m = " + siteModels.length);
        
        // 3. Initialization of output table.
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath("start_Codons_in_lncRNAs"));
        table.getColumnModel().addColumn("transcript_name", String.class);
        table.getColumnModel().addColumn("transcript_length", Integer.class);
        table.getColumnModel().addColumn("cds_length", Integer.class);
        table.getColumnModel().addColumn("test_for_exponentiality", String.class);
        table.getColumnModel().addColumn("statistics", Double.class);
        table.getColumnModel().addColumn("p_value", Double.class);
        table.getColumnModel().addColumn("frame", Integer.class);
        table.getColumnModel().addColumn("CDS_start_position", Integer.class);
        table.getColumnModel().addColumn("MAT1", Double.class);
        table.getColumnModel().addColumn("MAT2", Double.class);
        table.getColumnModel().addColumn("MAT3", Double.class);
        table.getColumnModel().addColumn("max_score", Double.class);
        
        // 4. Treatment of selected transcripts.
        int indexForOutput = 0;
        for( GeneTranscript gt : lncRnaTranscripts )
        {
            // 4.1. Create stopCodonPositions
            byte[] seqence = gt.getTranscriptSequence(pathToSequences);
            List<Integer> stopCodonPositionsList = new ArrayList<>();
            for( int i = 0; i < seqence.length - 2; i++ )
                if( Olig.isGivenOlig(seqence, i, STOP_CODONS[0]) || Olig.isGivenOlig(seqence, i, STOP_CODONS[1]) || Olig.isGivenOlig(seqence, i, STOP_CODONS[2]) )
                    stopCodonPositionsList.add(i);
            if( stopCodonPositionsList.size() < 5 ) continue;
            int[] stopCodonPositions = MatrixUtils.fromIntegerListToArray(stopCodonPositionsList);
            
            // 4.2 Treatment of different frames.
            String[] testTypes = new String[]{"NA", "NA", "NA"};
            double[][] statisticValuesAndpValues = new double[3][2];
            for( int frameIndex = 0; frameIndex < 3; frameIndex++ )
            {
                // 4.2.1. Identification of positionsInFrame.
                List<Integer> positionsInFrameList = new ArrayList<>();
                for(int i = 0; i < stopCodonPositions.length; i++ )
                {
                    int index = stopCodonPositions[i] - (stopCodonPositions[i] / 3) * 3;
                    if( index == frameIndex )
                        positionsInFrameList.add(stopCodonPositions[i]);
                }
                int[] positionsInFrame = MatrixUtils.fromIntegerListToArray(positionsInFrameList);
                if( positionsInFrame.length < 5 ) continue;
                
                // 4.2.2. Identify distancesBetweenNearestPositions and implement tests.
                double[] distancesBetweenNearestPositions = new double[positionsInFrame.length];
                for( int i = 0; i < positionsInFrame.length; i++ )
                    distancesBetweenNearestPositions[i] = i == 0 ? (double)positionsInFrame[i] : (double)(positionsInFrame[i] - positionsInFrame[i - 1]);
                    
                // 4.2.3. Calculate testTypes and statisticValuesAndpValues for current frame.
                double[] giniTest = TestsForExponentiality.getGiniTest(distancesBetweenNearestPositions);
                double[] coxAndOakesTest = TestsForExponentiality.getCoxAndOakesTest(distancesBetweenNearestPositions);
                if( giniTest[1] > 0.0 && giniTest[2] <= pValueThreshold )
                {
                    testTypes[frameIndex] = "Gini";
                    statisticValuesAndpValues[frameIndex][0] = giniTest[1];
                    statisticValuesAndpValues[frameIndex][1] = giniTest[2];
                }
                if( coxAndOakesTest[1] <= 0.0 || coxAndOakesTest[2] > pValueThreshold ) continue;
                if( testTypes[frameIndex].equals("Gini") && statisticValuesAndpValues[frameIndex][1] < coxAndOakesTest[2] ) continue;
                testTypes[frameIndex] = "Cox_and_Oakes";
                statisticValuesAndpValues[frameIndex][0] = coxAndOakesTest[1];
                statisticValuesAndpValues[frameIndex][1] = coxAndOakesTest[2];
            }
            if( testTypes[0].equals("NA") && testTypes[1].equals("NA") && testTypes[2].equals("NA") ) continue;
            
            // 4.3. Start codon identification.
            int transcriptLength = (int)(gt.getTranscriptLength() + 0.0001);
            String transcriptName = gt.getTranscriptName();
            for( int pos = 0; pos < seqence.length - 2; pos ++ )
            {
                if( Olig.isGivenOlig(seqence, pos, ATG) == false ) continue;
                if( pos - window / 2 < 0 || pos + window / 2 >= seqence.length - 1 ) continue;
                Interval cdsInterval = GeneTranscript.getCDS(seqence, pos);
                if( cdsInterval == null ) continue;
                int frameIndex = pos - (pos / 3) * 3;
                if( testTypes[frameIndex].equals("NA") ) continue;
                int cdsLength = cdsInterval.getLength(); 
                byte[] seq = Olig.getSubByteArray(seqence, pos - window / 2 - 5, window + 10);
                if( seq == null ) continue;
                Sequence lSequence = new LinearSequence("aaa", seq, alphabet);
                double score1 = SitePrediction.findBestSite(siteModels[0], lSequence, false).getScore();
                double score2 = SitePrediction.findBestSite(siteModels[1], lSequence, false).getScore();
                double score3 = SitePrediction.findBestSite(siteModels[2], lSequence, false).getScore();
                double maxScore = Math.max(score3, Math.max(score1, score2));
                if( maxScore < maxScoreThreshold ) continue;
                Object[] row = new Object[]{transcriptName, transcriptLength, cdsLength, testTypes[frameIndex],
                                            statisticValuesAndpValues[frameIndex][0], statisticValuesAndpValues[frameIndex][1],
                                            frameIndex, pos, score1, score2, score3, maxScore};
                TableDataCollectionUtils.addRow(table, Integer.toString(indexForOutput), row, true);
                log.info("indexForOutput = " + indexForOutput + " transcriptName = " + transcriptName + " p-value = " + statisticValuesAndpValues[frameIndex][1]);
                indexForOutput += 1;
            }
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }
    
    // Consider start codon scores for significantly exponential ORFrames
    private void predictStartCodonsAfterExponentialityTest(DataElementPath pathToSequences, DataElementPath pathToInputTable, DataElementPath pathToOutputs) throws Exception
    {
        double pValueThreshold = 0.01;
        int window = 100;
        // 1. Read data matrix with exponentiality tests.
        TableDataCollection table = pathToInputTable.getDataElement(TableDataCollection.class);
        String[] columnNames = new String[]{"Cox_and_Oakes_p_value_frame_1", "Cox_and_Oakes_p_value_frame_2", "Cox_and_Oakes_p_value_frame_3", "Cox_and_Oakes_p_value_minimal"};
        String columnNameWithTranscriptTypes = "transcript_type";
        Object[] objects = TableUtils.readDataSubMatrix(table, columnNames);
        String[] transcriptNames = (String[])objects[0];
        double[][] dataMatrix = (double[][])objects[1];
        String[] transcriptTypes = TableUtils.readGivenColumnInStringTable(table, columnNameWithTranscriptTypes);
        
        // 2. Create site models
        String[] pathsToMatrices = new String[]
                {"data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices_revised/near_start_codon_4_matrices_IPS_threshold_3.5/startCodon_1_iteration_15_revised",
                 "data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices_revised/near_start_codon_4_matrices_IPS_threshold_3.5/startCodon_2_iteration_15_revised",
                 "data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices_revised/near_start_codon_4_matrices_IPS_threshold_3.5/startCodon_3_iteration_15_revised"}; 
        List<IPSSiteModel> siteModelsList = new ArrayList<>();
        Alphabet alphabet = null;
        for( String pathToMatrix : pathsToMatrices)
        {
            FrequencyMatrix frequencyMatrix = DataElementPath.create(pathToMatrix).getDataElement(FrequencyMatrix.class);
            alphabet = frequencyMatrix.getAlphabet();
            siteModelsList.add((IPSSiteModel)SiteModelsComparison.getSiteModel(SiteModelsComparison.IPS_SITE_MODEL, frequencyMatrix, 0.01, window - 3));
        }
        IPSSiteModel[] siteModels = siteModelsList.toArray(new IPSSiteModel[0]);
        log.info("1. Creation of siteModels: n = " + siteModels.length);
        
        // 3. Select values for transcriptNameList, pValues, frameIndeces
        List<String> transcriptNameList = new ArrayList<>();
        List<Double> pValues = new ArrayList<>();
        List<Integer> frameIndeces = new ArrayList<>();
        for( int i = 0; i < dataMatrix.length; i++ )
        {
            log.info(" i = " + i + " transcriptTypes = " + transcriptTypes[i] + " dataMatrix[i][3] = " + dataMatrix[i][3]);
            if( ! transcriptTypes[i].equals("lncRNA") ) continue;
            if( Double.isNaN(dataMatrix[i][3]) || dataMatrix[i][3] > pValueThreshold ) continue;
            transcriptNameList.add(transcriptNames[i]);
            pValues.add(dataMatrix[i][3]);
            int frameIndex = -1;
            for( int j = 0; j < 3; j++ )
                if ( dataMatrix[i][j] == dataMatrix[i][3] )
                {
                    frameIndex = j;
                    break;
                }
            if( frameIndex < 0 )
                log.info("!!!!!!!!!! Error !!!!!");
            frameIndeces.add(frameIndex);
        }
        List<GeneTranscript> geneTranscriptList = GeneTranscript.readTranscriptsInEnsembl(transcriptNameList.toArray(new String[0]), pathToSequences, jobControl, 0, 40);
        log.info("2. Number of pre-selected transcripts, nn = " + transcriptNameList.size());
        
        // 4. Create output table.
        table = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath("start_Codons_in_lncRNAs"));
        table.getColumnModel().addColumn("transcript_name", String.class);
        table.getColumnModel().addColumn("transcript_length", Double.class);
        table.getColumnModel().addColumn("pvalue_for_exponentiality", Double.class);
        table.getColumnModel().addColumn("cds_length", Double.class);
        table.getColumnModel().addColumn("MAT1", Double.class);
        table.getColumnModel().addColumn("MAT2", Double.class);
        table.getColumnModel().addColumn("MAT3", Double.class);
        table.getColumnModel().addColumn("max_score", Double.class);

        //
        int index = 0;
        for( int i = 0; i < geneTranscriptList.size(); i++ )
        {
            GeneTranscript gt = geneTranscriptList.get(i);
            double transcriptLength = gt.getTranscriptLength();
            String transcriptName = transcriptNameList.get(i);
            int frameIndex = frameIndeces.get(i);
            double pvalueForExponentiality = pValues.get(i);
            byte[] seqence = gt.getTranscriptSequence(pathToSequences);

            ////////////////////////
            log.info("i = " + i + " frameIndex = " + frameIndex + " Name = " + transcriptName + " Length = " + transcriptLength + " pvalue = " + pvalueForExponentiality);

            for( int j = 0 + frameIndex; j < seqence.length - 2; j += 3 )
            {
                if( Olig.isGivenOlig(seqence, j, ATG) == false ) continue;
                if( j - window / 2 < 0 || j + window / 2 >= seqence.length - 1 ) continue;
                Interval cdsInterval = GeneTranscript.getCDS(seqence, j);
                if( cdsInterval == null ) continue;
                double cdsLength = (double)cdsInterval.getLength(); 
                byte[] seq = Olig.getSubByteArray(seqence, j - window / 2, window);
                Sequence sequence = new LinearSequence("aaa", seq, alphabet);
                double score1 = SitePrediction.findBestSite(siteModels[0], sequence, false).getScore();
                double score2 = SitePrediction.findBestSite(siteModels[1], sequence, false).getScore();
                double score3 = SitePrediction.findBestSite(siteModels[2], sequence, false).getScore();
                double maxScore = Math.max(score3, Math.max(score1, score2));
                Object[] row = new Object[]{transcriptName, transcriptLength,pvalueForExponentiality,
                                            cdsLength, score1, score2, score3, maxScore};
                TableDataCollectionUtils.addRow(table, Integer.toString(index), row, true);
                index += 1;
            }
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }
    
    // Gini and Cox_Oakes tests for exponentiality.
    private void treatmentOfCdsFrames(DataElementPath pathToSequences, DataElementPath pathToOutputs) throws Exception
    {
        // 1. Create list with mrna and lnc.
        List<GeneTranscript> geneTranscriptList = GeneTranscript.readTranscriptsInEnsembl(null, pathToSequences, null, 0, 0);
        List<GeneTranscript> mrnaAndLnc = new ArrayList<>();
        for( GeneTranscript gt : geneTranscriptList )
        {
            String geneType = gt.getGeneType();
            if( geneType.equals(Gene.PROTEIN_CODING) || geneType.equals(Gene.LINC_RNA ))
                mrnaAndLnc.add(gt);
        }
        log.info("size of mrnaAndLnc = " + mrnaAndLnc.size());
        
        // 2. Treatment of transcripts; calculation of data matrix
        List<String[]> stringDataMatrixList = new ArrayList<>();
        List<String> transcriptNamesList = new ArrayList<>();
        List<double[]> dataMatrixList = new ArrayList<>();
        for( GeneTranscript gt : mrnaAndLnc )
        {
            // 2.1. Create stopCodonPositions
            byte[] seqence = gt.getTranscriptSequence(pathToSequences);
            List<Integer> stopCodonPositionsList = new ArrayList<>();
            for( int i = 0; i < seqence.length - 2; i++ )
            {
                if( Olig.isGivenOlig(seqence, i, STOP_CODONS[0]) || Olig.isGivenOlig(seqence, i, STOP_CODONS[1]) || Olig.isGivenOlig(seqence, i, STOP_CODONS[2]) )
                    stopCodonPositionsList.add(i);
            }
            if( stopCodonPositionsList.size() < 5 ) continue;
            transcriptNamesList.add(gt.getTranscriptName());
            //stringDataMatrixList.add(new String[]{gt.getGeneType()});
            // String geneType = gt.getGeneType().equals(Gene.PROTEIN_CODING) ? "mRNA" : "lncRNA";
            stringDataMatrixList.add(new String[]{gt.getGeneType().equals(Gene.PROTEIN_CODING) ? "mRNA" : "lncRNA"});
            int[] stopCodonPositions = MatrixUtils.fromIntegerListToArray(stopCodonPositionsList);
            
            // 2.2 Identification of different frames.
            double[] matrixRow = new double[14];
            for( int frameIndex = 0; frameIndex < 3; frameIndex++ )
            {
                List<Integer> positionsInFrameList = new ArrayList<>();
                for(int i = 0; i < stopCodonPositions.length; i++ )
                {
                    int index = stopCodonPositions[i] - (stopCodonPositions[i] / 3) * 3;
                    if( index == frameIndex )
                        positionsInFrameList.add(stopCodonPositions[i]);
                }
                int[] positionsInFrame = MatrixUtils.fromIntegerListToArray(positionsInFrameList);
                double[] distancesBetweenNearestPositions = new double[positionsInFrame.length];
                for( int i = 0; i < positionsInFrame.length; i++ )
                    distancesBetweenNearestPositions[i] = i == 0 ? (double)positionsInFrame[i] : (double)(positionsInFrame[i] - positionsInFrame[i - 1]);
                if( distancesBetweenNearestPositions.length < 3 )
                    matrixRow[2 * frameIndex] = matrixRow[2 * frameIndex + 1] = matrixRow[7 + 2 * frameIndex] = matrixRow[7 + 2 * frameIndex + 1] = Double.NaN;
                else
                {
                    double[] giniTest = TestsForExponentiality.getGiniTest(distancesBetweenNearestPositions);
                    double[] coxAndOakesTest = TestsForExponentiality.getCoxAndOakesTest(distancesBetweenNearestPositions);

                    // temp
                    //giniTest = {statistic, statisticAsymptotic, pValue};
                    log.info("Gini: statistic = " + giniTest[0] + " statisticAsymptotic = " + giniTest[1] + " p-value = " + giniTest[2]);
                    log.info("CoxAndOakes: statistic = " + coxAndOakesTest[0] + " statisticAsymptotic = " + coxAndOakesTest[1] + " p-value = " + coxAndOakesTest[2]);

                    matrixRow[2 * frameIndex] = giniTest[1];
                    matrixRow[2 * frameIndex + 1] = giniTest[2];
                    matrixRow[7 + 2 * frameIndex] = coxAndOakesTest[1];
                    matrixRow[7 + 2 * frameIndex + 1] = coxAndOakesTest[2];
                }
            }
            double minPvalue = 10.0;
            for( int frameIndex = 0; frameIndex < 3; frameIndex++ )
                if( !Double.isNaN( matrixRow[2 * frameIndex + 1] ) )
                    minPvalue = Math.min(minPvalue, matrixRow[2 * frameIndex + 1]);
            matrixRow[6] = minPvalue > 9.99 ? Double.NaN : minPvalue;
            minPvalue = 10.0;
            for( int frameIndex = 0; frameIndex < 3; frameIndex++ )
                if( !Double.isNaN( matrixRow[7 + 2 * frameIndex + 1] ) )
                    minPvalue = Math.min(minPvalue, matrixRow[7 + 2 * frameIndex + 1]);
            matrixRow[13] = minPvalue > 9.99 ? Double.NaN : minPvalue;
            dataMatrixList.add(matrixRow);
        }
        String[] transcriptNames = transcriptNamesList.toArray(new String[0]);
        double[][] dataMatrix = dataMatrixList.toArray(new double[dataMatrixList.size()][]);
        String[][] stringDataMatrix = stringDataMatrixList.toArray(new String[stringDataMatrixList.size()][]);
        String[] namesOfDoubleColumns = new String[]{"Gini_statistic_frame_1", "Gini_p_value_frame_1", "Gini_statistic_frame_2", "Gini_p_value_frame_2", "Gini_statistic_frame_3", "Gini_p_value_frame_3", "Gini_p_value_minimal",
                                                     "Cox_and_Oakes_statistic_frame_1", "Cox_and_Oakes_p_value_frame_1", "Cox_and_Oakes_statistic_frame_2", "Cox_and_Oakes_p_value_frame_2", "Cox_and_Oakes_statistic_frame_3", "Cox_and_Oakes_p_value_frame_3", "Cox_and_Oakes_p_value_minimal"};
        String[] namesOfStringColumns = new String[]{"transcript_type"};
        TableDataCollection table = TableUtils.writeDoubleAndString(dataMatrix, stringDataMatrix, transcriptNames, namesOfDoubleColumns, namesOfStringColumns, pathToOutputs, "tests_for_exponentiality");
    }
   
    private void treatmentOfCds(DataElementPath pathToSequences) throws Exception
    {
        String[] transcriptNames = new String[]{"ENST00000634594", "ENST00000634601", "ENST00000634626", "ENST00000634658"};
        List<GeneTranscript> geneTranscriptList = GeneTranscript.readTranscriptsInEnsembl(transcriptNames, pathToSequences, null, 0, 0);
        for( GeneTranscript gt : geneTranscriptList )
        {
            byte[] seqence = gt.getTranscriptSequence(pathToSequences);
            for( int orderOfStartCodon = 1; orderOfStartCodon < 10; orderOfStartCodon++ )
            {
                Interval interval = GeneTranscript.getCDSWithGivenOrderOfStartCodon(seqence, orderOfStartCodon);
                if( interval == null )
                    log.info("orderOfStartCodon = " + orderOfStartCodon + " interval = null");
                else
                    log.info("orderOfStartCodon = " + orderOfStartCodon + " cds = " + interval.getFrom() + " " + interval.getTo() + " cds length = " + interval.getLength());
            }
            
            List<Integer> atgPositions = new ArrayList<>();
            List<Integer> stopCodonPositions = new ArrayList<>();
            for( int i = 0; i < seqence.length - 2; i++ )
            {
                if( Olig.isGivenOlig(seqence, i, ATG) )
                    atgPositions.add(i);
                if( Olig.isGivenOlig(seqence, i, STOP_CODONS[0]) || Olig.isGivenOlig(seqence, i, STOP_CODONS[1]) || Olig.isGivenOlig(seqence, i, STOP_CODONS[2]) )
                    stopCodonPositions.add(i);
            }
            String sequenceAsString = new String(seqence);
            log.info("transcriptName = " + gt.getTranscriptName());
            log.info("n = " + sequenceAsString.length() + " transcriptSequence = " + sequenceAsString);
            log.info("n = " + atgPositions.size() + " atgPositions = " + atgPositions);
            log.info("n = " + stopCodonPositions.size() + " stopCodonPositions = " + stopCodonPositions);
            int[] atgPositions_ = MatrixUtils.fromIntegerListToArray(atgPositions);
            int[] stopCodonPositions_ = MatrixUtils.fromIntegerListToArray(stopCodonPositions);
            tretmentOfFrames(atgPositions_);
            tretmentOfFrames(stopCodonPositions_);
        }
    }
    
    private void tretmentOfFrames(int[] positions)
    {
        if( positions.length < 2 ) return;
        List<Integer> frame1 = new ArrayList<>(), frame2 = new ArrayList<>(), frame3 = new ArrayList<>();
        for(int i = 0; i < positions.length; i++ )
        {
            // int x = i == 0 ? positions[0] : positions[i] - positions[i - 1];
            int frameIndex = positions[i] - (positions[i] / 3) * 3;
            switch(frameIndex)
            {
                case 0 : frame1.add(positions[i]); break;
                case 1 : frame2.add(positions[i]); break;
                case 2 : frame3.add(positions[i]); break;
            }
        }
        int[] frame1_ = MatrixUtils.fromIntegerListToArray(frame1), frame2_ = MatrixUtils.fromIntegerListToArray(frame2), frame3_ = MatrixUtils.fromIntegerListToArray(frame3);
        for( int i = frame1_.length - 1; i >= 0; i-- )
            frame1_[i] = i == 0 ? frame1_[i] : frame1_[i] - frame1_[i - 1];
        for( int i = frame2_.length - 1; i >= 0; i-- )
            frame2_[i] = i == 0 ? frame2_[i] : frame2_[i] - frame2_[i - 1]; 
        for( int i = frame3_.length - 1; i >= 0; i-- )
            frame3_[i] = i == 0 ? frame3_[i] : frame3_[i] - frame3_[i - 1]; 
        MatrixUtils.printVector(log, "frame1_", frame1_);
        MatrixUtils.printVector(log, "frame2_", frame2_);
        MatrixUtils.printVector(log, "frame3_", frame3_);
    }
    
    // List<String> list = new ArrayList<>(Arrays.asList(transcriptNames));
    //List<String> list = Arrays.asList(transcriptNames);
    
    private void calculateAndWriteMatricesScores(DataElementPath pathToOutputs)
    {
        int aug_number = 12;
        String[] pathsToMatrices = new String[]
                    {"data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices_revised/near_start_codon_4_matrices_IPS_threshold_3.5/startCodon_1_iteration_15_revised",
                     "data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices_revised/near_start_codon_4_matrices_IPS_threshold_3.5/startCodon_2_iteration_15_revised",
                     "data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices_revised/near_start_codon_4_matrices_IPS_threshold_3.5/startCodon_3_iteration_15_revised"}; 
        int window = 97;
        
        // 1. Create siteModels.
        List<IPSSiteModel> siteModels0 = new ArrayList<>();
        Alphabet alphabet = null;
        for( String pathToMatrix : pathsToMatrices)
        {
            FrequencyMatrix frequencyMatrix = DataElementPath.create(pathToMatrix).getDataElement(FrequencyMatrix.class);
            alphabet = frequencyMatrix.getAlphabet();
            siteModels0.add((IPSSiteModel)SiteModelsComparison.getSiteModel(SiteModelsComparison.IPS_SITE_MODEL, frequencyMatrix, 0.01, window));
        }        IPSSiteModel[] siteModels = siteModels0.toArray(new IPSSiteModel[0]);
        log.info("1. Creation of siteModels: n = " + siteModels.length);
        
        // 2. Read table with mRNAs.
        DataElementPath pathToTable = DataElementPath.create("data/Collaboration/yura_test/Data/_RiboSeq/Paper2_for_JBCB/_Samples/human_38_protein_cording");
        String[] columnNamesForSubMatrix = new String[]
                {"length of transcript", "lg(length of transcript)", "5'UTR length", "lg(5'UTR length)",
                 "3'UTR length", "lg(3'UTR length)", "CDS length", "lg(CDS length)"};
        String stringColumnName = "Sequence sample: near start codons [-55, 55]";
        Object[] objects = TableUtils.readDataSubMatrixAndStringColumn(pathToTable, columnNamesForSubMatrix, stringColumnName);
        String[] sequenceNames = (String[])objects[0];
        double[][] dataMatrix = (double[][])objects[1];
        String[] sequenceSample = (String[])objects[2];
        Sequence[] sequences = SequenceSampleUtils.transformSequenceSample(sequenceNames, sequenceSample, alphabet);
        log.info("2. Read table with mRNAs, n = " + dataMatrix.length);
        
        // 3. Initialization of 2 new tables.
        TableDataCollection table1 = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath("human_38_protein_and_lincs"));
        TableDataCollection table2 = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath("human_38_lincs"));
        for( String s : columnNamesForSubMatrix )
        {
            table1.getColumnModel().addColumn(s, Double.class);
            table2.getColumnModel().addColumn(s, Double.class);
        }
        table1.getColumnModel().addColumn(stringColumnName, String.class);
        table2.getColumnModel().addColumn(stringColumnName, String.class);
        table1.getColumnModel().addColumn("Transcript_types", String.class);
        table2.getColumnModel().addColumn("Transcript_types", String.class);
        table1.getColumnModel().addColumn("Transcript_subtypes", String.class);
        table2.getColumnModel().addColumn("Transcript_subtypes", String.class);
        for( int i = 0; i < 3; i++ )
        {
            table1.getColumnModel().addColumn("MAT" + Integer.toString(i + 1), Double.class);
            table2.getColumnModel().addColumn("MAT" + Integer.toString(i + 1), Double.class);
        }
        table1.getColumnModel().addColumn("max_score", Double.class);
        table2.getColumnModel().addColumn("max_score", Double.class);
        log.info("3. Initialization of 2 new tables");
        
        // 4. Creation of part of table1.
        for( int i = 0; i < dataMatrix.length; i++ )
        {
            Object[] objs = new Object[columnNamesForSubMatrix.length + 7];
            for( int j = 0; j < columnNamesForSubMatrix.length; j++ )
                objs[j] = dataMatrix[i][j];
            objs[columnNamesForSubMatrix.length + 0] = sequenceSample[i];
            objs[columnNamesForSubMatrix.length + 1] = "mRNA";
            objs[columnNamesForSubMatrix.length + 2] = "mRNA";
            for( int j = 0; j < 3; j++ )
            {
                double score = Double.NaN;
                if( sequences[i] != null )
                {
                    Site bestSite = SitePrediction.findBestSite(siteModels[j], sequences[i], false);
                    score = bestSite.getScore();
                }
                objs[columnNamesForSubMatrix.length + 3 + j] = score;
            }
            objs[columnNamesForSubMatrix.length + 6] = sequences[i] == null ? null : MatrixUtils.getMaximalValue(new double[]{(double)objs[columnNamesForSubMatrix.length + 3], (double)objs[columnNamesForSubMatrix.length + 4], (double)objs[columnNamesForSubMatrix.length + 5]})[0];
            TableDataCollectionUtils.addRow(table1, sequenceNames[i], objs, true);
        }
        log.info("4. Creation of part of table1.");
        
        // 5. Create 1-st and 2-nd tables.
        for( int index = 0; index < aug_number; index++ )
        {
            log.info("5. Linc-sample No = " + index);
            // 5.1. Read table with lincs.
            pathToTable = DataElementPath.create("data/Collaboration/yura_test/Data/_RiboSeq/Paper2_for_JBCB/_Samples/human_38_lincRNA_start_codon__" + Integer.toString(index + 1));
            objects = TableUtils.readDataSubMatrixAndStringColumn(pathToTable, columnNamesForSubMatrix, stringColumnName);
            sequenceNames = (String[])objects[0];
            dataMatrix = (double[][])objects[1];
            sequenceSample = (String[])objects[2];
            sequences = SequenceSampleUtils.transformSequenceSample(sequenceNames, sequenceSample, alphabet);
            
            // 5.2. Creation of 1-st and 2-nd tables.
            for( int i = 0; i < dataMatrix.length; i++ )
            {
                Object[] objs = new Object[columnNamesForSubMatrix.length + 7];
                for( int j = 0; j < columnNamesForSubMatrix.length; j++ )
                    objs[j] = dataMatrix[i][j];
                objs[columnNamesForSubMatrix.length + 0] = sequenceSample[i];
                objs[columnNamesForSubMatrix.length + 1] = "lncRNA";
                objs[columnNamesForSubMatrix.length + 2] = "lncRNA_" + Integer.toString(index + 1);
                for( int j = 0; j < 3; j++ )
                {
                    double score = Double.NaN;
                    if( sequences[i] != null )
                    {
                        Site bestSite = SitePrediction.findBestSite(siteModels[j], sequences[i], false);
                        score = bestSite.getScore();
                    }
                    objs[columnNamesForSubMatrix.length + 3 + j] = score;
                }
                objs[columnNamesForSubMatrix.length + 6] = sequences[i] == null ? null : MatrixUtils.getMaximalValue(new double[]{(double)objs[columnNamesForSubMatrix.length + 3], (double)objs[columnNamesForSubMatrix.length + 4], (double)objs[columnNamesForSubMatrix.length + 5]})[0];
                TableDataCollectionUtils.addRow(table1, sequenceNames[i] + "_" + Integer.toString(index + 1), objs, true);
                TableDataCollectionUtils.addRow(table2, sequenceNames[i] + "_" + Integer.toString(index + 1), objs, true);
            }
        }
        table1.finalizeAddition();
        CollectionFactoryUtils.save(table1);
        table2.finalizeAddition();
        CollectionFactoryUtils.save(table2);
        
        
//        IPSSiteModel siteModel = nameAndSiteModel.get(matrixName);
//        int window = siteModel.getWindow();
//        int startPosition = mrnaFeatureName.contains(MATRIX_FOR_START_CODON) ? cdsFromAndTo.getFrom() - window / 2 - 1 : cdsFromAndTo.getTo() - window / 2 - 3;
//        byte[] region = Olig.getSubByteArray(transcriptSequence, startPosition, window + 2);
//        if( region == null ) return null;
//        Sequence sequence = new LinearSequence(getTranscriptName(), region, siteModel.getAlphabet());
//        Site bestSite = SitePrediction.findBestSite(siteModel, sequence, false);
//        return bestSite.getScore();
    }

    private void treatmentOfAUGinLidersOfProteinCodingTranscripts(DataElementPath pathToSequences, DataElementPath pathToOutputs) throws Exception
    {
        // 1. Calculate frequencies of AUGs
        List<GeneTranscript> geneTranscriptList = GeneTranscript.readTranscriptsInEnsembl(null, pathToSequences, jobControl, 0, 40);
        geneTranscriptList = GeneTranscript.removeNonProteinCodingTranscriptsWithShortLiders(geneTranscriptList, jobControl, 40, 65);
        TObjectIntMap<String> frequencies = new TObjectIntHashMap<>();
        int from = 65, to = 95, difference = to - from, iJobControl = 0;
        for( GeneTranscript gt : geneTranscriptList )
        {
            if( jobControl != null )
                jobControl.setPreparedness(from + ++iJobControl * difference / geneTranscriptList.size());
            byte[] transcriptSequence = gt.getTranscriptSequence(pathToSequences);
            int numberOfAUGinLider = gt.getNumberOfAUGinLider(transcriptSequence);
            frequencies.adjustOrPutValue(Integer.toString(numberOfAUGinLider), 1, 1);
        }
        
        // 2. Create and write table
        int totalNumber = 0;
        for( int num : frequencies.values() )
            totalNumber += num;
        int size = frequencies.size(), index = 0;
        String[] names = new String[size];
        double[][] frequenciesAndPercentages = new double[size][];
        for( String augNumberAsString : frequencies.keySet() )
        {
            names[index] = augNumberAsString;
            int freq = frequencies.get(augNumberAsString);
            frequenciesAndPercentages[index++] = new double[]{freq, 100.0 * freq / totalNumber};
        }
        TableUtils.writeDoubleTable(frequenciesAndPercentages, names, new String[]{"frequency of AUGs in mRNA liders", "percentage of AUGs in mRNA liders"}, pathToOutputs, "percentage_of_AUGs_in_mRNA_liders");
        if( jobControl != null )
            jobControl.setPreparedness(100);
    }
    
    private void normalizationOfTeFromDifferentDatasets(DataElementPath pathToTableWithDataMatrix, DataElementPath pathToOutputs)
    {
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        Map<String, String> mapWithNames = TableUtils.readGivenColumnInStringTable(pathToTableWithDataMatrix, "DataSet");
        Map<String, Double> mapWithValues = TableUtils.readGivenColumnInDoubleTableAsMap(pathToTableWithDataMatrix, "lg(TE)");
        Set<String> set = new HashSet<>();
        for( String s : mapWithNames.values() )
            set.add(s);
        Map<String, double[]> mapWithMeans = new HashMap<>();
        for( String dataSetName : set )
        {
            List<Double> values = new ArrayList<>();
            for( Entry<String, String> entry : mapWithNames.entrySet() )
            {
                String name = entry.getKey();
                String dataSetN = entry.getValue();
                if( ! dataSetN.equals(dataSetName) ) continue;
                Double value = mapWithValues.get(name);
                values.add(value);
            }
            double[] ms = Stat.getMeanAndSigma1(values);
            mapWithMeans.put(dataSetName, ms);
        }

        // names of rows and normed values
        String[] rowNames = new String[mapWithNames.size()];
        double[][] normedValues = new double[mapWithNames.size()][2];
        int index = 0;
        for( Entry<String, String> entry : mapWithNames.entrySet() )
        {
            String rowName = entry.getKey();
            String dataSetName = entry.getValue();
            double value = mapWithValues.get(rowName);
            rowNames[index] = rowName;
            double[] meanAndSigma = mapWithMeans.get(dataSetName);
            normedValues[index][0] = value - meanAndSigma[0];
            normedValues[index][1] = (value - meanAndSigma[0]) / meanAndSigma[1];
            index++;
        }
        TableUtils.writeDoubleTable(normedValues, rowNames, new String[]{"lg(TE) - mean(lg(TE))", "(lg(TE) - mean(lg(TE_)) : sigma(lg(TE))"}, pathToOutputs, "temp_normedValues");
    }

    
    private void predictStartCodonsForGivenTranscripts(DataElementPath pathToSequences, DataElementPath pathToOutputs) throws Exception
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath("site_predictions"));
        table.getColumnModel().addColumn("Transcript name", String.class);
        table.getColumnModel().addColumn("Matrix name", String.class);
        table.getColumnModel().addColumn("Site position", Integer.class);
        table.getColumnModel().addColumn("Site score", Double.class);

        // 1. Create site models
        String siteModelType = SiteModelsComparison.IPS_SITE_MODEL;
        Integer window = 50;
        FrequencyMatrix frequencyMatrix1 = DataElementPath.create("data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices/nnnccrnnatggnnn/startCodon_1_iteration_15").getDataElement(FrequencyMatrix.class);
        FrequencyMatrix frequencyMatrix2 = DataElementPath.create("data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices/nnnccrnnatggnnn/startCodon_2_iteration_15").getDataElement(FrequencyMatrix.class);
        FrequencyMatrix frequencyMatrix3 = DataElementPath.create("data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices/nnnccrnnatggnnn/startCodon_3_iteration_15").getDataElement(FrequencyMatrix.class);
        FrequencyMatrix frequencyMatrix4 = DataElementPath.create("data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices/nnnccrnnatggnnn/startCodon_4_iteration_15").getDataElement(FrequencyMatrix.class);
        FrequencyMatrix[] frequencyMatrices = new FrequencyMatrix[]{frequencyMatrix1, frequencyMatrix2, frequencyMatrix3, frequencyMatrix4};
        log.info("O.K. 1");
        SiteModel[] siteModels = new SiteModel[frequencyMatrices.length];
        String[] matrixNames = new String[frequencyMatrices.length];
        for( int i = 0; i < frequencyMatrices.length; i++ )
        {
            siteModels[i] = SiteModelsComparison.getSiteModel(siteModelType, frequencyMatrices[i], 2.5, window);
            matrixNames[i] = frequencyMatrices[i].getName();
        }
        Alphabet alphabet = frequencyMatrix1.getAlphabet();
        log.info("O.K. 2");

        // 2.
        log.info("Creation of ParticularRiboSeq object and geneTranscriptList");
        DataElementPath pathToRiboSeq = DataElementPath.create("data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/Samples/Our_Ingolia1/Summary table");
        DataElementPath pathToMrnaSeq = DataElementPath.create("data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/Samples/Our_Ingolia1/mRNA Transcript read counts");
        ParticularRiboSeq prs = new ParticularRiboSeq(ParticularRiboSeq.TWO_TABLES_FOR_TIE, null, pathToMrnaSeq, "Count", -0.1, pathToRiboSeq, "Reads number", -0.1, "Summit offset", "Transcript name", pathToSequences, GeneTranscript.EACH_START_CODON, 1, jobControl, 0, 100);
        List<GeneTranscript> geneTranscriptList = prs.getGeneTranscriptList();
        log.info("O.K. 3");
        
        // 3.
        log.info("Prediction of sites");
        int index = 0;
        for( GeneTranscript gt : geneTranscriptList )
        {
            String transcriptName = gt.getTranscriptName();
            log.info("transcriptName = " + transcriptName);
            byte[] seq = gt.getTranscriptSequence(pathToSequences);
            Sequence sequence = new LinearSequence(transcriptName, seq, alphabet);
            for( int i = 0; i < siteModels.length; i++ )
            {
                SqlTrack temporaryTrack = SqlTrack.createTrack(pathToOutputs.getChildPath("temporary"), null);
                siteModels[i].findAllSites(sequence, temporaryTrack);
                temporaryTrack.finalizeAddition();
                DataCollection<Site> sites = temporaryTrack.getAllSites();
                for( Site site : sites )
                {
                    int from = site.getFrom(), to = site.getTo();
                    int position = (from + to) / 2;
                    double score = site.getScore();
                    Object[] rowElements = new Object[]{transcriptName, matrixNames[i], position, score};
                    TableDataCollectionUtils.addRow(table, Integer.toString(index++), rowElements, true);
                    if( index >= 1000 )
                    {
                        table.finalizeAddition();
                        CollectionFactoryUtils.save(table);
                        return;
                    }
                }
            }
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }
    
    private Object[] getBestSitesForSequenceSample(DataElementPath pathToTableWithData)
    {
        // 1. Create site models
        String siteModelType = SiteModelsComparison.IPS_SITE_MODEL;
        Integer window = 50;
        FrequencyMatrix frequencyMatrix1 = DataElementPath.create("data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices/nnnccrnnatggnnn/startCodon_1_iteration_15").getDataElement(FrequencyMatrix.class);
        FrequencyMatrix frequencyMatrix2 = DataElementPath.create("data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices/nnnccrnnatggnnn/startCodon_2_iteration_15").getDataElement(FrequencyMatrix.class);
        FrequencyMatrix frequencyMatrix3 = DataElementPath.create("data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices/nnnccrnnatggnnn/startCodon_3_iteration_15").getDataElement(FrequencyMatrix.class);
        FrequencyMatrix frequencyMatrix4 = DataElementPath.create("data/Collaboration/yura_test/Data/PEOPLE/Volkova_2/ANSWERS/ANS07_matrices/nnnccrnnatggnnn/startCodon_4_iteration_15").getDataElement(FrequencyMatrix.class);
        FrequencyMatrix[] frequencyMatrices = new FrequencyMatrix[]{frequencyMatrix1, frequencyMatrix2, frequencyMatrix3, frequencyMatrix4};
        log.info("O.K. 1");
        SiteModel[] siteModels = new SiteModel[frequencyMatrices.length];
        String[] matrixNames = new String[frequencyMatrices.length];
        for( int i = 0; i < frequencyMatrices.length; i++ )
        {
            siteModels[i] = SiteModelsComparison.getSiteModel(siteModelType, frequencyMatrices[i], 2.0, window);
            matrixNames[i] = frequencyMatrices[i].getName();
        }
        Alphabet alphabet = frequencyMatrix1.getAlphabet();
        log.info("O.K. 2");
        
        // 2. Calculate data = scores of best sites
         Object[] objects = readSequencesAndSequenceNamesInTable(pathToTableWithData, "Sequence sample: near start codons [-50, 50]");
         String[] sequenceNames = (String[])objects[0];
         String[] sequenceSample = (String[])objects[1];
         log.info("O.K. 3");
         double[][] data = new double[sequenceSample.length][siteModels.length];
         for( int i = 0; i < sequenceSample.length; i++ )
         {
             if( sequenceSample[i] == null || sequenceSample[i].equals("") )
             {
                 for( int j = 0; j < siteModels.length; j++ )
                     data[i][j] = Double.NaN;
                 continue;
             }
             Sequence sequence = new LinearSequence(sequenceNames[i], sequenceSample[i], alphabet);
             for( int j = 0; j < siteModels.length; j++ )
             {
                 data[i][j] = SiteModelsComparisonUtils.findBestSite(sequence, false, siteModels[j]).getScore();
             }
         }
         return new Object[]{sequenceNames, matrixNames, data};
    }
    
    // temp : it is modification of 'readSequencesAndSequenceNamesInTable()' from MatrixDerivation.java
    private Object[] readSequencesAndSequenceNamesInTable(DataElementPath pathToTableWithData, String nameOfTableColumnWithSequences)
    {
        TableDataCollection table = pathToTableWithData.getDataElement(TableDataCollection.class);
        String[] sequenceSample = TableUtils.readGivenColumnInStringTable(table, nameOfTableColumnWithSequences);
        String[] sequenceNames = TableUtils.readRowNamesInTable(table);
        return new Object[] {sequenceNames, sequenceSample};
    }

    private TableDataCollection getInformationOnGTRDtracks(DataElementPath pathToOutputs)
    {
        TableDataCollection tab = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath("informationOnSequenceSet"));
        tab.getColumnModel().addColumn("Name of sequence set", String.class);
        tab.getColumnModel().addColumn("TF-class", String.class);
        tab.getColumnModel().addColumn("cell line", String.class);
        tab.getColumnModel().addColumn("antibody", String.class);
        DataElementPath pathToInputTable = DataElementPath.create("data/Collaboration/yura_test/Data/PEOPLE/KEL/ANS01_tables_for_article/MACS/percentage_25");
        TableDataCollection table = pathToInputTable.getDataElement(TableDataCollection.class);
        String[] rowNames = table.names().collect( Collectors.toList() ).toArray( new String[0] );
        String[] peakNames = TableUtils.readGivenColumnInStringTable(table, "Name of sequence set");
        DataElementPath pathToPeaks = DataElementPath.create("databases/GTRD/Data/peaks/macs");
        for( int i = 0; i < peakNames.length; i++ )
        {
            DataElementPath pathToPeak = pathToPeaks.getChildPath(peakNames[i]);
            TrackInfo trackInfo = new TrackInfo(pathToPeak);
            String cellLine = trackInfo.getCellLine();
            String tfClass = trackInfo.getTfClass();
            String antibody = trackInfo.getAntibody();
            Object[] row = new Object[]{peakNames[i], tfClass, cellLine, antibody};
            TableDataCollectionUtils.addRow(tab, rowNames[i], row, true);
        }
        tab.finalizeAddition();
        CollectionFactoryUtils.save(tab);
        return tab;
    }

    
    private TableDataCollection getMeanEntropiesForGivenMatrices(DataElementPath pathToOutputs)
    {
        TableDataCollection tab = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath("meanEntropies"));
        tab.getColumnModel().addColumn("Matrix name", String.class);
        tab.getColumnModel().addColumn("Matrix length", Integer.class);
        tab.getColumnModel().addColumn("Mean entropy", Double.class);
        tab.getColumnModel().addColumn("Sigma of entropy", Double.class);
        DataElementPath pathToInputTable = DataElementPath.create("data/Collaboration/yura_test/Data/PEOPLE/KEL/ANS01_tables_for_article/MACS/percentage_25");
        TableDataCollection table = pathToInputTable.getDataElement(TableDataCollection.class);
        String[] rowNames = table.names().collect( Collectors.toList() ).toArray( new String[0] );
        String[] matrixNames = TableUtils.readGivenColumnInStringTable(table, "Matrix name");
        DataElementPath pathToMatrices = DataElementPath.create("databases/TRANSFAC(R) 2012.4/Data/matrix");
        for( int i = 0; i < matrixNames.length; i++ )
        {
            DataElementPath pathToMatrix = pathToMatrices.getChildPath(matrixNames[i]);
            FrequencyMatrix freqMatrix = pathToMatrix.getDataElement(FrequencyMatrix.class);
            WeightMatrixModel wmModel = new WeightMatrixModel(SiteModelsComparison.WEIGHT_MATRIX_MODEL, null, freqMatrix, 0.0);
            double[][] weights = wmModel.getWeights();
            if( i == 0 )
            {
                log.info("matrix name = " + matrixNames[i] + " dim(weights) = " + weights.length + " x " + weights[0].length);
                log.info("weights = ");
                for( int ii = 0; ii < weights.length; ii++ )
                {
                    String s = DoubleStreamEx.of(weights[ii]).joining( " " );
                    log.info("ii = " + ii + " weights[ii][] = " + s);
                }
            }
            double[] entropiesForColumnsOfGivenMatrix = new double[weights.length];
            for( int ii = 0; ii < weights.length; ii++ )
            {
                for( int j = 0; j < 4; j++ )
                    if( weights[ii][j] > 0.0 )
                        entropiesForColumnsOfGivenMatrix[ii] -= weights[ii][j] * Math.log(weights[ii][j]);
            }
            double[] meanAndSigma = Stat.getMeanAndSigma(entropiesForColumnsOfGivenMatrix);
            Object[] row = new Object[]{matrixNames[i], weights.length, meanAndSigma[0], meanAndSigma[1]};
            TableDataCollectionUtils.addRow(tab, rowNames[i], row, true);
        }
        tab.finalizeAddition();
        CollectionFactoryUtils.save(tab);
        return tab;
    }
    
    // 0. temporary for Kel
    /***
    log.info("Create temporary tables for Kel");
    boolean isForKel = true;
    int[] percentages = new int[]{5, 15, 25, 35, 100};
    for(int i = 0; i < percentages.length; i++ )
    {
        getSummaryTableWithAUCsForKel(pathToCollectionOfFolders, percentages[i], pathToOutputs, "percentage_" + String.valueOf(percentages[i]));
    }
    if( isForKel ) return pathToOutputs.getDataCollection();
    ***/
    
    // temporary method: create particular table for Kel
    private TableDataCollection getSummaryTableWithAUCsForKel(DataElementPath pathToCollectionOfFolders, int percentage, DataElementPath pathToOutputs, String tableName)
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath(tableName));
        table.getColumnModel().addColumn("TF-class", String.class);
        table.getColumnModel().addColumn("Matrix name", String.class);
        table.getColumnModel().addColumn("Name of sequence set", String.class);
        table.getColumnModel().addColumn("Size of sequence set", Integer.class);
        table.getColumnModel().addColumn("AUC for MATCH model", Double.class);
        table.getColumnModel().addColumn("AUC for Common multiplicative model", Double.class);
        table.getColumnModel().addColumn("AUC for Common additive model", Double.class);
        String inputTableName = SiteModelsComparisonUtils.AUCS;
        ru.biosoft.access.core.DataElementPath[] paths = pathToCollectionOfFolders.getChildrenArray();
        int ii = 0;
        for( DataElementPath path : paths )
        {
            Object[] row = new Object[]{null, null, null, null, Double.NaN, Double.NaN, Double.NaN};
            TableDataCollection tableForReading = path.getChildPath(inputTableName).getDataElement(TableDataCollection.class);
            int[] percentages = TableUtils.readGivenColumnInIntegerTable(tableForReading, "Percentage of best sites");
            int index = IntStreamEx.ofIndices( percentages, p -> p == percentage ).findFirst().orElse( -1 );
            if( index >= 0 )
            {
                String[] tfClasses = TableUtils.readGivenColumnInStringTable(tableForReading, "TF-class");
                String[] matrixNames = TableUtils.readGivenColumnInStringTable(tableForReading, "Matrix name");
                String[] peaksNames = TableUtils.readGivenColumnInStringTable(tableForReading, "Name of sequence set");
                int[] sizes = TableUtils.readGivenColumnInIntegerTable(tableForReading, "Size of sequence set");
                double[] aucsForMatch = TableUtils.readGivenColumnInDoubleTableAsArray(tableForReading, "AUC for MATCH model");
                double[] aucsForCommonMultiplicative = TableUtils.readGivenColumnInDoubleTableAsArray(tableForReading, "AUC for Common multiplicative model");
                double[] aucsForCommonAdditive = TableUtils.readGivenColumnInDoubleTableAsArray(tableForReading, "AUC for Common additive model");
                row = new Object[]{tfClasses[index], matrixNames[index], peaksNames[index], sizes[index], aucsForMatch[index], aucsForCommonMultiplicative[index], aucsForCommonAdditive[index]};
            }
            TableDataCollectionUtils.addRow(table, String.valueOf(ii++), row, true);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return table;
    }

    public static class LincRNAandMrnaFeaturesParameters extends AbstractAnalysisParameters
    {
        private BasicGenomeSelector dbSelector;
        private Species species = Species.getDefaultSpecies(null);
        DataElementPath pathToTableWithCoeffficients;
        private DataElementPath pathToFile;
        private DataElementPath outputPath;
        
        public LincRNAandMrnaFeaturesParameters()
        {
            setDbSelector(new BasicGenomeSelector());
        }
        
        @PropertyName(MessageBundle.PN_DB_SELECTOR)
        @PropertyDescription(MessageBundle.PD_DB_SELECTOR)
        public BasicGenomeSelector getDbSelector()
        {
            return dbSelector;
        }
        public void setDbSelector(BasicGenomeSelector dbSelector)
        {
            Object oldValue = this.dbSelector;
            this.dbSelector = dbSelector;
            dbSelector.setParent(this);
            firePropertyChange("dbSelector", oldValue, dbSelector);
        }
        
        @PropertyName(MessageBundle.PN_SPECIES)
        @PropertyDescription(MessageBundle.PD_SPECIES)
        public Species getSpecies()
        {
            return species;
        }

        public void setSpecies(Species species)
        {
            Object oldValue = this.species;
            this.species = species;
            firePropertyChange("species", oldValue, species);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_TABLE_WITH_COEFFICIENTS)
        @PropertyDescription(MessageBundle.PD_PATH_TO_TABLE_WITH_COEFFICIENTS)
        public DataElementPath getPathToTableWithCoeffficients()
        {
            return pathToTableWithCoeffficients;
        }
        public void setPathToTableWithCoeffficients(DataElementPath pathToTableWithCoeffficients)
        {
            Object oldValue = this.pathToTableWithCoeffficients;
            this.pathToTableWithCoeffficients = pathToTableWithCoeffficients;
            firePropertyChange("pathToTableWithCoeffficients", oldValue, pathToTableWithCoeffficients);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_FILE)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FILE)
        public DataElementPath getPathToFile()
        {
            return pathToFile;
        }
        public void setPathToFile(DataElementPath pathToFile)
        {
            Object oldValue = this.pathToFile;
            this.pathToFile = pathToFile;
            firePropertyChange("pathToFile", oldValue, pathToFile);
        }
        
        @PropertyName(MessageBundle.PN_OUTPUT_PATH)
        @PropertyDescription(MessageBundle.PD_OUTPUT_PATH)
        public DataElementPath getOutputPath()
        {
            return outputPath;
        }
        public void setOutputPath(DataElementPath outputPath)
        {
            Object oldValue = this.outputPath;
            this.outputPath = outputPath;
            firePropertyChange("outputPath", oldValue, outputPath);
        }
    }
    
    public static class LincRNAandMrnaFeaturesParametersBeanInfo extends BeanInfoEx2<LincRNAandMrnaFeaturesParameters>
    {
        public LincRNAandMrnaFeaturesParametersBeanInfo()
        {
            super(LincRNAandMrnaFeaturesParameters.class);
        }


        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH));
            //property("pathToTableWithCoeffficients").inputElement(TableDataCollection.class).add();

            // new
            // property("pathToFile").inputElement(FileDataElement.class).add();
            
            property("outputPath").outputElement(FolderCollection.class).add();
        }
    }
}

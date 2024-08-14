/* $Id$ */

package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.gtrd.utils.GeneActivityUtils;
import biouml.plugins.gtrd.utils.EnsemblUtils.GeneTranscript;
import biouml.plugins.gtrd.utils.GeneActivityUtils.PromoterRegion;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrixString;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral;
import biouml.plugins.machinelearning.utils.DataMatrixString.DataMatrixChar;
import biouml.plugins.machinelearning.utils.MatrixUtils.MatrixTransformation;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Track;
import ru.biosoft.util.ConstantResourceBundle;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 *
 */
public class RnaSeqAnalysis extends AnalysisMethodSupport<RnaSeqAnalysis.RnaSeqAnalysisParameters>
{
    public static final String OPTION_01 = "OPTION_01 : Conversion of initial RNA-Seq file into processed file.";
    public static final String OPTION_02 = "OPTION_02 : Create indicator {0, 1}-matrix indicating the overlapping of binding regions and promoter regions and write it to file. Ensembl TSSs are used for determination of promoter regions.";
    
    public RnaSeqAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new RnaSeqAnalysisParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        String option = parameters.getOption();
        DataElementPath pathToOutputFolder = parameters.getPathToOutputFolder();
        DataCollectionUtils.createFoldersForPath(pathToOutputFolder.getChildPath(""));
        switch( option )
        {
            case OPTION_01 : log.info(OPTION_01);
                             DataElementPath pathToInputFile = parameters.getParametersForOption01().getPathToFile();
                             DataElementPath pathToSequences = parameters.getParametersForOption02().getDbSelector().getSequenceCollectionPath();
                             DataElementPath pathToTranscriptsCollection = pathToSequences.getRelativePath("../../Data/transcript");
                             
                             // TODO: temporary for article
                             //implementOption01ForseveralFiles(pathToTranscriptsCollection, pathToOutputFolder);
                             
                             implementOption01(pathToInputFile, pathToTranscriptsCollection, pathToOutputFolder, pathToInputFile.getName() + "_processed");
                             break;
            case OPTION_02 : log.info(OPTION_02);
                             pathToSequences = parameters.getParametersForOption02().getDbSelector().getSequenceCollectionPath();
                             PromoterRegion[] promoterRegions = parameters.getParametersForOption02().getPromoterRegions();
                             DataElementPath pathToFolderWithTracks = parameters.getParametersForOption02().getPathToFolderWithTracks();
                             String[] trackNames = parameters.getParametersForOption02().getTrackNames();
                             DataElementPath pathToFileWithTsss = parameters.getParametersForOption02().getPathToFile();
                             implementOption02(pathToSequences, pathToFolderWithTracks, trackNames, promoterRegions, pathToFileWithTsss, pathToOutputFolder);
                             break;
        }
        return pathToOutputFolder.getDataCollection();
    }
    
    /************************************************************************/
    /************************ For  OPTION_01 ********************************/
    /************************************************************************/
     
    private void implementOption01(DataElementPath pathToInputFile, DataElementPath pathToTranscriptsCollection, DataElementPath pathToOutputFolder, String fileName)
    {
        // 1. 
        String[] columnNames = TableAndFileUtils.getColumnNames(pathToInputFile);
        int index = ArrayUtils.indexOf(columnNames, "gene_id");
        if( index >= 0 )
            columnNames = (String[])ArrayUtils.remove(columnNames, index);
        index = ArrayUtils.indexOf(columnNames, "length");
        if( index >= 0 )
            columnNames = (String[])ArrayUtils.remove(columnNames, index);
        index = ArrayUtils.indexOf(columnNames, "transcript_id(s)");
        if( index >= 0 )
            columnNames = (String[])ArrayUtils.remove(columnNames, index);
        DataMatrix dm = new DataMatrix(pathToInputFile, columnNames);
        log.info("1. O.K. : Read dataMatrix for transcripts in input file and remove columns 'gene_id' , 'transcript_id(s)' and 'length'");
        
        // 2.
        String[] ids = dm.getRowNames();
        List<Integer> list = new ArrayList<>();
        for( int i = 0; i < ids.length; i++ )
        {
            String substring = ids[i].substring(0, 3);
            if( substring.equals("ENS") )
                list.add(i);
        }
        dm = (DataMatrix)DataMatrix.splitRowWise(dm, null, null, UtilsGeneral.fromListIntegerToArray(list))[0];
        log.info("2. O.K. : Remove non-Ensembl transcript or gene IDs; dim = " + dm.getSize());
        
        // 3.
        ids = dm.getRowNames();
        for( int i = 0; i < ids.length; i++ )
            ids[i] = TextUtil.split(ids[i], '.')[0];
        dm.replaceRowNames(ids);
        log.info("3. O.K. : Simplify transcript or gene IDs");
        
        // 4. lg-transformation
        DataMatrix dmWithLgIntensities = dm.getSubDataMatrixColumnWise(new String[]{"TPM", "FPKM"});
        double[][] matrix = MatrixTransformation.getLgMatrixWithReplacement(dmWithLgIntensities.getMatrix());
        dmWithLgIntensities = new DataMatrix(dm.getRowNames(), new String[]{"TPM_lg", "FPKM_lg"}, matrix);
        dm = DataMatrix.concatinateDataMatricesColumnWise(new DataMatrix[]{dm, dmWithLgIntensities});
        log.info("4. O.K. : Calculate lg-transformed TPM and FPKM");
        
        // 5. Save data matrix for Ensembl genes
        char letter = dm.getRowNames()[0].charAt(3);
        boolean isGene = letter == 'G' ? true : false;
        if( isGene )
        {
            dm.writeDataMatrix(true, pathToOutputFolder, fileName, log);
            return;
        }
        
        // 6.
        GeneTranscript[] gts = GeneTranscript.getGeneTranscripts(ids, pathToTranscriptsCollection);
        list = new ArrayList<>();
        List<GeneTranscript> transcriptList = new ArrayList<>(); 
        for( int i = 0; i < ids.length; i++ )
            if( gts[i] != null )
            {
                list.add(i);
                transcriptList.add(gts[i]);
            }
        gts = transcriptList.toArray(new GeneTranscript[0]);
        dm = (DataMatrix)DataMatrix.splitRowWise(dm, null, null, UtilsGeneral.fromListIntegerToArray(list))[0];
        log.info("6. O.K. : Read transcipts in Ensembl; remove non-existing transcripts from data matrix; dim = " + dm.getSize());

        // temporary
//        for( int i = 0; i < 30; i++ )
//        {
//            String transcriptName = gts[i].getTranscriptName(), chromosome = gts[i].getChromosome(), geneID = gts[i].getGeneID(), geneName = gts[i].getGeneName(), geneType = gts[i].getTranscriptType();
//            Interval fromAndTo = gts[i].getFromAndTo(), cdsFromAndTo = gts[i].getCdsFromAndTo();
//            int strand = gts[i].getStrand(), tss = gts[i].getTss();
//            Interval[] exonPositions = gts[i].getExonPositions();
//            log.info("transcriptName = " + transcriptName + " chromosome = " + chromosome + " geneID = " + geneID + " geneName = " + geneName + " geneType = " + geneType);
//            log.info("strand = " + strand + " TSS = " + tss);
//            if( fromAndTo == null ) log.info("fromAndTo == null");
//            else log.info("fromAndTo = " + fromAndTo.getFrom() + " " + fromAndTo.getTo());
//            if( cdsFromAndTo == null ) log.info("cdsFromAndTo == null");
//            else log.info("cdsFromAndTo = " + cdsFromAndTo.getFrom() + " " + cdsFromAndTo.getTo());
//            if( exonPositions == null ) log.info("exonPositions == null");
//            else
//                for( int j = 0; j < exonPositions.length; j++ )
//                    log.info("j = "  + j + " exonPositions[j] = " + exonPositions[j].getFrom() + " " + exonPositions[j].getTo()); 
//        }
        
        // 7. Save resulted file for Ensembl transcripts.
        String string = transformResultsToString(gts, dm);
        TableAndFileUtils.writeStringToFile(string, pathToOutputFolder, fileName, log);
    }

    // TODO: temporary; only for article
    private void implementOption01ForseveralFiles(DataElementPath pathToTranscriptsCollection, DataElementPath pathToOutputFolder)
    {
        DataElementPath pathToInputFolder = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/RNA_seq/ENCODE_26_cell_lines/DATA01");
        String[] fileNames = pathToInputFolder.getDataCollection(DataElement.class).getNameList().toArray(new String[0]);
        for( int ii = 0; ii < fileNames.length; ii++ )
        {
            log.info("ii = " + ii + " fileNames[ii] = " + fileNames[ii]);
            DataElementPath pathToInputFile = pathToInputFolder.getChildPath(fileNames[ii]);
            implementOption01(pathToInputFile, pathToTranscriptsCollection, pathToOutputFolder, fileNames[ii] + "_processed");
        }
    }

    private String transformResultsToString(GeneTranscript[] gts, DataMatrix dataMatrix)
    {
        String[] columnNames = dataMatrix.getColumnNames(), rowNames = dataMatrix.getRowNames();
        double[][] matrix = dataMatrix.getMatrix();
        StringBuilder builder = new StringBuilder();
        builder.append("ID\tchromosome\tstrand\tTSS_start\tTSS_start_+1\ttranscript_end\ttranscript_length\tgene_ID\tgene_name\ttranscript_type");
        for( String s : columnNames )
            builder.append('\t').append(s);
        for( int i = 0; i < rowNames.length; i++ )
        {
            int tss = gts[i].getTss();
            builder.append('\n').append(rowNames[i]).append('\t').append(gts[i].getChromosome()).append('\t').append(gts[i].getStrand()).append('\t').append(tss).append('\t').append(tss + 1).append('\t').append(gts[i].getTranscriptEnd()).append('\t').append(gts[i].getTranscriptLength()).append('\t').append(gts[i].getGeneID()).append('\t').append(gts[i].getGeneName()).append('\t').append(gts[i].getTranscriptType());
            for( double x : matrix[i] )
                builder.append('\t').append(x);
        }
        return builder.toString();
    }
    
    /************************************************************************/
    /************************ For  OPTION_02 ********************************/
    /************************************************************************/
    
    private void implementOption02(DataElementPath pathToSequences, DataElementPath pathToFolderWithTracks, String[] trackNames, PromoterRegion[] promoterRegions, DataElementPath pathToFileWithTsss, DataElementPath pathToOutputFolder)
    {
        Object[] objects = GeneActivityUtils.calculateIndicatorAndFrequencyMatrices(pathToSequences, pathToFolderWithTracks, trackNames, promoterRegions, pathToFileWithTsss, jobControl);
        DataMatrixChar indicatorMatrix = (DataMatrixChar)objects[0];
        DataMatrix frequencyMatrix = (DataMatrix)objects[1];
        DataMatrix dataMatrix = convertIndicatorMatrix(indicatorMatrix);
        indicatorMatrix = null;
        dataMatrix.addAnotherDataMatrixColumnWise(frequencyMatrix);
        dataMatrix.addAnotherDataMatrixColumnWise(new DataMatrix(pathToFileWithTsss, new String[]{"TPM", "FPKM", "TPM_lg", "FPKM_lg"}));
        DataMatrixString dms = new DataMatrixString(pathToFileWithTsss, new String[]{"transcript_type"});
        dataMatrix.writeDataMatrix(true, dms, pathToOutputFolder, "data_matrix", log);
    }
    
    private static DataMatrix convertIndicatorMatrix(DataMatrixChar indicatorMatrix)
    {
        char[][] matrixChar = indicatorMatrix.getMatrix();
        double[][] matrix = new double[matrixChar.length][matrixChar[0].length];
        for( int i = 0; i < matrixChar.length; i++ )
            for( int j = 0; j < matrixChar[0].length; j++ )
                matrix[i][j] = matrixChar[i][j] == '0' ? 0.0 : 1.0;
        return new DataMatrix(indicatorMatrix.getRowNames(), indicatorMatrix.getColumnNames(), matrix);
    }
    
    public static String[] getAvailableOptions()
    {
        return new String[]{OPTION_01, OPTION_02};
    }
    
    /************************************************************************/
    /************************** Utils for AnalysisMethodSupport *************/
    /************************************************************************/
    
    public class MessageBundle extends ConstantResourceBundle
    {
        public static final String PN_OPTION = "Option";
        public static final String PD_OPTION = "Please, select option (i.e. select the concrete session of given analysis).";
        
        public static final String PN_DB_SELECTOR = "Sequences collection";
        public static final String PD_DB_SELECTOR = "Select a source of nucleotide sequences";
        
        public static final String PN_PATH_TO_FOLDER_WITH_TRACKS = "Path to folder with tracks";
        public static final String PD_PATH_TO_FOLDER_WITH_TRACKS = "Path to folder with tracks";
        
        public static final String PN_TRACK_NAMES = "Track names";
        public static final String PD_TRACK_NAMES = "Please, select track names";
        
        public static final String PN_PATH_TO_FILE = "Path to file";
        public static final String PD_PATH_TO_FILE = "Path to input file";

        public static final String PN_PARAMETERS_FOR_OPTION_01 = "Parameters for OPTION_01";
        public static final String PD_PARAMETERS_FOR_OPTION_01 = "Parameters for OPTION_01";
        
        public static final String PN_PARAMETERS_FOR_OPTION_02 = "Parameters for OPTION_02";
        public static final String PD_PARAMETERS_FOR_OPTION_02 = "Parameters for OPTION_02";
        
        public static final String PN_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        public static final String PD_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
    }

    public static class AllParameters extends AbstractAnalysisParameters
    {
        private BasicGenomeSelector dbSelector;
        private String option = OPTION_01;
        private PromoterRegion[] promoterRegions = new PromoterRegion[]{new PromoterRegion()};
        private DataElementPath pathToFolderWithTracks;
        private String[] trackNames;
        private DataElementPath pathToFile;
        private DataElementPath pathToOutputFolder;
        
        public AllParameters()
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
        
        @PropertyName(MessageBundle.PN_OPTION)
        @PropertyDescription(MessageBundle.PD_OPTION)
        public String getOption()
        {
            return option;
        }
        public void setOption(String option)
        {
            Object oldValue = this.option;
            this.option = option;
            firePropertyChange("*", oldValue, option);
        }
        
        @PropertyName("Promoter regions")
        public PromoterRegion[] getPromoterRegions()
        {
            return promoterRegions;
        }
        public void setPromoterRegions(PromoterRegion[] promoterRegions)
        {
            Object oldValue = this.promoterRegions;
            this.promoterRegions = promoterRegions;
            firePropertyChange("promoterRegions", oldValue, promoterRegions);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_TRACKS)
        @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_TRACKS)
        public DataElementPath getPathToFolderWithTracks()
        {
            return pathToFolderWithTracks;
        }
        public void setPathToFolderWithTracks(DataElementPath pathToFolderWithTracks)
        {
            Object oldValue = this.pathToFolderWithTracks;
            this.pathToFolderWithTracks = pathToFolderWithTracks;
            firePropertyChange("pathToFolderWithTracks", oldValue, pathToFolderWithTracks);
        }
        
        @PropertyName(MessageBundle.PN_TRACK_NAMES)
        @PropertyDescription(MessageBundle.PD_TRACK_NAMES)
        public String[] getTrackNames()
        {
            return trackNames;
        }
        public void setTrackNames(String[] trackNames)
        {
            Object oldValue = this.trackNames;
            this.trackNames = trackNames;
            firePropertyChange("trackNames", oldValue, trackNames);
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

        @PropertyName(MessageBundle.PN_PATH_TO_OUTPUT_FOLDER)
        @PropertyDescription(MessageBundle.PD_PATH_TO_OUTPUT_FOLDER)
        public DataElementPath getPathToOutputFolder()
        {
            return pathToOutputFolder;
        }
        public void setPathToOutputFolder(DataElementPath pathToOutputFolder)
        {
            Object oldValue = this.pathToOutputFolder;
            this.pathToOutputFolder = pathToOutputFolder;
            firePropertyChange("pathToOutputFolder", oldValue, pathToOutputFolder);
        }
    }
    
    public static class PromoterRegionBeanInfo extends BeanInfoEx2<PromoterRegion>
    {
        public PromoterRegionBeanInfo()
        {
            super(PromoterRegion.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("startPosition");
            add("finishPosition");
        }
    }
    
    public static class ParametersForOption01 extends AllParameters
    {}
    
    public static class ParametersForOption01BeanInfo extends BeanInfoEx2<ParametersForOption01>
    {
        public ParametersForOption01BeanInfo()
        {
            super(ParametersForOption01.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            add(DataElementPathEditor.registerInput("pathToFile", beanClass, FileDataElement.class, true));
        }
    }
    
    public static class ParametersForOption02 extends AllParameters
    {}
    
    public static class ParametersForOption02BeanInfo extends BeanInfoEx2<ParametersForOption02>
    {
        public ParametersForOption02BeanInfo()
        {
            super(ParametersForOption02.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInputChild("pathToFolderWithTracks", beanClass, Track.class, true));
            add("trackNames", TrackNamesSelector.class);
            add(DataElementPathEditor.registerInput("pathToFile", beanClass, FileDataElement.class, true));
            add("dbSelector");
            add("promoterRegions");
        }
    }
    
    public static class RnaSeqAnalysisParameters extends AllParameters
    {
        ParametersForOption01 parametersForOption01;
        ParametersForOption02 parametersForOption02;

        public RnaSeqAnalysisParameters()
        {
            setParametersForOption01(new ParametersForOption01());
            setParametersForOption02(new ParametersForOption02());
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_01)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_01)
        public ParametersForOption01 getParametersForOption01()
        {
            return parametersForOption01;
        }
        public void setParametersForOption01(ParametersForOption01 parametersForOption01)
        {
            Object oldValue = this.parametersForOption01;
            this.parametersForOption01 = parametersForOption01;
            firePropertyChange("parametersForOption01", oldValue, parametersForOption01);
        }
        
        @PropertyName(MessageBundle.PN_PARAMETERS_FOR_OPTION_02)
        @PropertyDescription(MessageBundle.PD_PARAMETERS_FOR_OPTION_02)
        public ParametersForOption02 getParametersForOption02()
        {
            return parametersForOption02;
        }
        public void setParametersForOption02(ParametersForOption02 parametersForOption02)
        {
            Object oldValue = this.parametersForOption02;
            this.parametersForOption02 = parametersForOption02;
            firePropertyChange("parametersForOption02", oldValue, parametersForOption02);
        }
        
        public boolean areParametersForOption01Hidden()
        {
            return( ! getOption().equals(OPTION_01) );
        }
        
        public boolean areParametersForOption02Hidden()
        {
            return( ! getOption().equals(OPTION_02) );
        }
    }
    
    public static class OptionSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return getAvailableOptions();
        }
    }
    
    public static class TrackNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataCollection<DataElement> tracks = ((ParametersForOption02)getBean()).getPathToFolderWithTracks().getDataCollection(DataElement.class);
                String[] trackNames = tracks.getNameList().toArray(new String[0]);
                Arrays.sort(trackNames, String.CASE_INSENSITIVE_ORDER);
                return trackNames;
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select folder with tracks)"};
            }
            catch( Exception e )
            {
                return new String[]{"(folder doesn't contain the tracks)"};
            }
        }
    }
    
    public static class RnaSeqAnalysisParametersBeanInfo extends BeanInfoEx2<RnaSeqAnalysisParameters>
    {
        public RnaSeqAnalysisParametersBeanInfo()
        {
            super(RnaSeqAnalysisParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("option", beanClass), OptionSelector.class);
            addHidden("parametersForOption01", "areParametersForOption01Hidden");
            addHidden("parametersForOption02", "areParametersForOption02Hidden");
            add(DataElementPathEditor.registerOutput("pathToOutputFolder", beanClass, FolderCollection.class, true));
        }
    }
}

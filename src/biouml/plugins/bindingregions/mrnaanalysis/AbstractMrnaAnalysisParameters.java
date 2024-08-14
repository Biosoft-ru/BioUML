
package biouml.plugins.bindingregions.mrnaanalysis;

import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.plugins.bindingregions.mrnautils.GeneTranscript;
import biouml.plugins.bindingregions.mrnautils.ParticularRiboSeq;
import biouml.plugins.bindingregions.resources.MessageBundle;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 *
 */
public abstract class AbstractMrnaAnalysisParameters extends AbstractAnalysisParameters
{
    private BasicGenomeSelector dbSelector; 
    private String dataSetName = ParticularRiboSeq.TWO_TABLES_FOR_TE;
    private String[] mrnaAndRiboSeqFeatureNames;
    private DataElementPath pathToTableWithMrnaSeqData;
    private String columnNameWithMrnaSeqReadsNumber;
    private double mrnaSeqReadsThreshold;
    private DataElementPath pathToTableWithRiboSeqData;
    private String columnNameWithRiboSeqReadsNumber;
    private double riboSeqReadsThreshold;
    private String columnNameWithStartCodonPositions;
    private String columnNameWithTranscriptNames;
    private String startCodonType = GeneTranscript.CANONICAL_START_CODON;
    private int orderOfStartCodon = 1;
    private DataElementPath pathToFolderWithDataSets;    //     = DataElementPath.create("data/Collaboration/yura_test/Data/PEOPLE/Volkova"); // O.K.
    private DataElementPath pathToOutputTable;
    
    public AbstractMrnaAnalysisParameters()
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

    @PropertyName(MessageBundle.PN_DATA_SET)
    @PropertyDescription(MessageBundle.PD_DATA_SET)
    public String getDataSetName()
    {
        return dataSetName;
    }
    public void setDataSetName(String dataSetName)
    {
        Object oldValue = this.dataSetName;
        this.dataSetName = dataSetName;
        firePropertyChange("*", oldValue, dataSetName);
        firePropertyChange("*", null, null);
    }
    
    @PropertyName(MessageBundle.PN_MRNA_AND_RIBO_SEQ_FEATURE_NAMES)
    @PropertyDescription(MessageBundle.PD_MRNA_AND_RIBO_SEQ_FEATURE_NAMES)
    public String[] getMrnaAndRiboSeqFeatureNames()
    {
        return mrnaAndRiboSeqFeatureNames;
    }
    public void setMrnaAndRiboSeqFeatureNames(String[] mrnaAndRiboSeqFeatureNames)
    {
        Object oldValue = this.mrnaAndRiboSeqFeatureNames;
        this.mrnaAndRiboSeqFeatureNames = mrnaAndRiboSeqFeatureNames;
        firePropertyChange("*", oldValue, mrnaAndRiboSeqFeatureNames);
    }
    
    @PropertyName(MessageBundle.PN_PATH_TO_TABLE_WITH_MRNA_SEQ)
    @PropertyDescription(MessageBundle.PD_PATH_TO_TABLE_WITH_MRNA_SEQ)
    public DataElementPath getPathToTableWithMrnaSeqData()
    {
        return pathToTableWithMrnaSeqData;
    }
    public void setPathToTableWithMrnaSeqData(DataElementPath pathToTableWithMrnaSeqData)
    {
        Object oldValue = this.pathToTableWithMrnaSeqData;
        this.pathToTableWithMrnaSeqData = pathToTableWithMrnaSeqData;
        firePropertyChange("pathToTableWithMrnaSeqData", oldValue, pathToTableWithMrnaSeqData);
        setColumnNameWithMrnaSeqReadsNumber(ColumnNameSelector.getNumericColumn(pathToTableWithMrnaSeqData, getColumnNameWithMrnaSeqReadsNumber()));
    }
    
    @PropertyName(MessageBundle.PN_COLUMN_NAME_WITH_MRNA_SEQ_READS_NUMBER)
    @PropertyDescription(MessageBundle.PD_COLUMN_NAME_WITH_MRNA_SEQ_READS_NUMBER)
    public String getColumnNameWithMrnaSeqReadsNumber()
    {
        return columnNameWithMrnaSeqReadsNumber;
    }
    public void setColumnNameWithMrnaSeqReadsNumber(String columnNameWithMrnaSeqReadsNumber)
    {
        Object oldValue = this.columnNameWithMrnaSeqReadsNumber;
        this.columnNameWithMrnaSeqReadsNumber = columnNameWithMrnaSeqReadsNumber;
        firePropertyChange("columnNameWithMrnaSeqReadsNumber", oldValue, columnNameWithMrnaSeqReadsNumber);
    }

    @PropertyName(MessageBundle.PN_MRNA_SEQ_READS_THRESHOLD)
    @PropertyDescription(MessageBundle.PD_MRNA_SEQ_READS_THRESHOLD)
    public double getMrnaSeqReadsThreshold()
    {
        return mrnaSeqReadsThreshold;
    }
    public void setMrnaSeqReadsThreshold(double mrnaSeqReadsThreshold)
    {
        Object oldValue = this.mrnaSeqReadsThreshold;
        this.mrnaSeqReadsThreshold = mrnaSeqReadsThreshold;
        firePropertyChange("mrnaSeqReadsThreshold", oldValue, mrnaSeqReadsThreshold);
    }
    
    @PropertyName(MessageBundle.PN_PATH_TO_TABLE_WITH_RIBO_SEQ)
    @PropertyDescription(MessageBundle.PD_PATH_TO_TABLE_WITH_RIBO_SEQ)
    public DataElementPath getPathToTableWithRiboSeqData()
    {
        return pathToTableWithRiboSeqData;
    }
    public void setPathToTableWithRiboSeqData(DataElementPath pathToTableWithRiboSeqData)
    {
        Object oldValue = this.pathToTableWithRiboSeqData;
        this.pathToTableWithRiboSeqData = pathToTableWithRiboSeqData;
        firePropertyChange("pathToTableWithRiboSeqData", oldValue, pathToTableWithRiboSeqData);
        setColumnNameWithRiboSeqReadsNumber(ColumnNameSelector.getNumericColumn(pathToTableWithRiboSeqData, getColumnNameWithRiboSeqReadsNumber()));
    }
    
    @PropertyName(MessageBundle.PN_COLUMN_NAME_WITH_RIBO_SEQ_READS_NUMBER)
    @PropertyDescription(MessageBundle.PD_COLUMN_NAME_WITH_RIBO_SEQ_READS_NUMBER)
    public String getColumnNameWithRiboSeqReadsNumber()
    {
        return columnNameWithRiboSeqReadsNumber;
    }
    public void setColumnNameWithRiboSeqReadsNumber(String columnNameWithRiboSeqReadsNumber)
    {
        Object oldValue = this.columnNameWithRiboSeqReadsNumber;
        this.columnNameWithRiboSeqReadsNumber = columnNameWithRiboSeqReadsNumber;
        firePropertyChange("columnNameWithRiboSeqReadsNumber", oldValue, columnNameWithRiboSeqReadsNumber);
    }
    
    @PropertyName(MessageBundle.PN_RIBO_SEQ_READS_THRESHOLD)
    @PropertyDescription(MessageBundle.PD_RIBO_SEQ_READS_THRESHOLD)
    public double getRiboSeqReadsThreshold()
    {
        return riboSeqReadsThreshold;
    }
    public void setRiboSeqReadsThreshold(double riboSeqReadsThreshold)
    {
        Object oldValue = this.riboSeqReadsThreshold;
        this.riboSeqReadsThreshold = riboSeqReadsThreshold;
        firePropertyChange("riboSeqReadsThreshold", oldValue, riboSeqReadsThreshold);
    }
    
    @PropertyName(MessageBundle.PN_COLUMN_NAME_WITH_START_CODON_POSITIONS)
    @PropertyDescription(MessageBundle.PD_COLUMN_NAME_WITH_START_CODON_POSITIONS)
    public String getColumnNameWithStartCodonPositions()
    {
        return columnNameWithStartCodonPositions;
    }
    public void setColumnNameWithStartCodonPositions(String columnNameWithStartCodonPositions)
    {
        Object oldValue = this.columnNameWithStartCodonPositions;
        this.columnNameWithStartCodonPositions = columnNameWithStartCodonPositions;
        firePropertyChange("columnNameWithStartCodonPositions", oldValue, columnNameWithStartCodonPositions);
    }
    
    @PropertyName(MessageBundle.PN_COLUMN_NAME_WITH_TRANSCRIPT_NAMES)
    @PropertyDescription(MessageBundle.PD_COLUMN_NAME_WITH_TRANSCRIPT_NAMES)
    public String getColumnNameWithTranscriptNames()
    {
        return columnNameWithTranscriptNames;
    }
    public void setColumnNameWithTranscriptNames(String columnNameWithTranscriptNames)
    {
        Object oldValue = this.columnNameWithTranscriptNames;
        this.columnNameWithTranscriptNames = columnNameWithTranscriptNames;
        firePropertyChange("columnNameWithTranscriptNames", oldValue, columnNameWithTranscriptNames);
    }

    @PropertyName(MessageBundle.PN_START_CODON_TYPE)
    @PropertyDescription(MessageBundle.PD_START_CODON_TYPE)
    public String getStartCodonType()
    {
        return startCodonType;
    }
    public void setStartCodonType(String startCodonType)
    {
        Object oldValue = this.startCodonType;
        this.startCodonType = startCodonType;
        firePropertyChange("startCodonType", oldValue, startCodonType);
    }
    
    @PropertyName(MessageBundle.PN_START_CODON_ORDER)
    @PropertyDescription(MessageBundle.PN_START_CODON_ORDER)
    public int getOrderOfStartCodon()
    {
        return orderOfStartCodon;
    }
    public void setOrderOfStartCodon(int orderOfStartCodon)
    {
        Object oldValue = this.orderOfStartCodon;
        this.orderOfStartCodon = orderOfStartCodon;
        firePropertyChange("orderOfStartCodon", oldValue, orderOfStartCodon);
    }
    
    @PropertyName(MessageBundle.PN_PATH_TO_FOLDER_WITH_DATA_SETS)
    @PropertyDescription(MessageBundle.PD_PATH_TO_FOLDER_WITH_DATA_SETS)
    public DataElementPath getPathToFolderWithDataSets()
    {
        return pathToFolderWithDataSets;
    }
    public void setPathToFolderWithDataSets(DataElementPath pathToFolderWithDataSets)
    {
        Object oldValue = this.pathToFolderWithDataSets;
        this.pathToFolderWithDataSets = pathToFolderWithDataSets;
        firePropertyChange("pathToFolderWithDataSets", oldValue, pathToFolderWithDataSets);
    }
    
    @PropertyName(MessageBundle.PN_OUTPUT_TABLE_PATH)
    @PropertyDescription(MessageBundle.PD_OUTPUT_TABLE_PATH)
    public DataElementPath getPathToOutputTable()
    {
        return pathToOutputTable;
    }
    public void setPathToOutputTable(DataElementPath pathToOutputTable)
    {
        Object oldValue = this.pathToOutputTable;
        this.pathToOutputTable = pathToOutputTable;
        firePropertyChange("pathToOutputTable", oldValue, pathToOutputTable);
    }

    /***
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
    ***/
    
    public static class FullDataSetEditor extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return ParticularRiboSeq.getAllAvailableDataSetNames();
        }
    }

    public static class StartCodonTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[]{GeneTranscript.CANONICAL_START_CODON, GeneTranscript.NONCANONICAL_START_CODON, GeneTranscript.EACH_START_CODON};
        }
    }
    
    public static class MatrixTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return new String[]{GeneTranscript.MATRIX_FOR_START_CODON, GeneTranscript.MATRIX_FOR_STOP_CODON};
        }
    }

    public static class MrnaFeatureNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                return GeneTranscript.getMrnaFeatureNames();
            }
            catch( Exception e )
            {
                return new String[] {"(please select feature names)"};
            }
        }
    }

    public boolean isPathToFolderWithDataSetsHidden()
    {
        String dataSetName = getDataSetName();
        return(dataSetName.equals(ParticularRiboSeq.TWO_TABLES_FOR_TE) || dataSetName.equals(ParticularRiboSeq.TWO_TABLES_FOR_TIE) || dataSetName.equals(ParticularRiboSeq.ALL_TRANSCRIPTS_WITH_PROTEIN_CODING_SET) || dataSetName.equals(ParticularRiboSeq.ALL_PROTEIN_CODING_WITH_PREDICTED_CDS) || dataSetName.equals(ParticularRiboSeq.ALL_TRANSCRIPTS_WITH_LINC_RNA));
    }

    public boolean isStartCodonTypeHidden()
    {
        String dataSetName = getDataSetName();
        return(dataSetName.equals(ParticularRiboSeq.ALL_TRANSCRIPTS_WITH_LINC_RNA) || dataSetName.equals(ParticularRiboSeq.ALL_PROTEIN_CODING_WITH_PREDICTED_CDS));
    }
    
    public boolean isOrderOfStartCodonHidden()
    {
        String dataSetName = getDataSetName();
        return( ! (dataSetName.equals(ParticularRiboSeq.ALL_TRANSCRIPTS_WITH_LINC_RNA) || dataSetName.equals(ParticularRiboSeq.ALL_PROTEIN_CODING_WITH_PREDICTED_CDS)));
    }
    
    public boolean isDbSelectorHidden()
    {
        return(getDataSetName().equals(ParticularRiboSeq.INGOLIA_GSE30839_DATA_SET));
    }
}

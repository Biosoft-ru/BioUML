package biouml.plugins.bindingregions.mrnaanalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.Olig;
import biouml.plugins.bindingregions.utils.TableUtils;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author yura
 * Calculation of olig frequencies in sequence samples and compare them
 */
public class OligsInSequenceSamples extends AnalysisMethodSupport<OligsInSequenceSamples.OligsInSequenceSamplesParameters>
{
    public OligsInSequenceSamples(DataCollection<?> origin, String name)
    {
        super(origin, name, new OligsInSequenceSamplesParameters());
    }
    
    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        log.info("Calculation of olig frequencies in sequence samples and compare them");
        int oligLength = Math.max(1, parameters.getOligLength());
        DataElementPath pathToTableWithData = parameters.getPathToTableWithData();
        String nameOfTableColumnWithSequences = parameters.getNameOfTableColumnWithSequences();
        String nameOfTableColumnWithSampleNames = parameters.getNameOfTableColumnWithSampleNames();
        DataElementPath pathToOutputTable = parameters.getPathToOutputTable();

        // 1.
        log.info("Read sequence samples in table");
        log.info("oligLength = " + oligLength + " nameOfTableColumnWithSequences = " + nameOfTableColumnWithSequences + " nameOfTableColumnWithSampleNames = " + nameOfTableColumnWithSampleNames);
        Object[] objects = readSequenceSamplesAndSampleNamesInTable(pathToTableWithData, nameOfTableColumnWithSequences, nameOfTableColumnWithSampleNames);
        String[] sequencesForEachObject = (String[])objects[0], sampleNameForEachObject = (String[])objects[1];
        log.info("size of sequencesForEachObject = " + sequencesForEachObject.length);
        for( int i = 0; i < sequencesForEachObject.length; i++ )
            log.info("i = " + i + " sequenceSample = " + sequencesForEachObject[i] + " sampleNames = " + sampleNameForEachObject[i]);
        jobControl.setPreparedness(30);

        // 2.
        log.info("Calculate olig frequencies and write them into table");
        Map<String, List<byte[]>> sampleNameAndSequenceSample = getSampleNameAndSequenceSample(sequencesForEachObject, sampleNameForEachObject);
        jobControl.setPreparedness(40);
        TableDataCollection table = writeTableWithOligFequenciesAndThierDiffernces(oligLength, sampleNameAndSequenceSample, pathToOutputTable.getParentPath(), pathToOutputTable.getName());
        jobControl.setPreparedness(100);
        return table;
    }
    
    private Map<String, List<byte[]>> getSampleNameAndSequenceSample(String[] sequencesForEachObject, String[] sampleNameForEachObject)
    {
        Map<String, List<byte[]>> result = new HashMap<>();
        for( int i = 0; i < sequencesForEachObject.length; i++ )
        {
            byte[] sequence = sequencesForEachObject[i].toLowerCase().getBytes();
            result.computeIfAbsent(sampleNameForEachObject[i], key -> new ArrayList<>()).add(sequence);
        }
        return result;
    }
    
    private Object[] readSequenceSamplesAndSampleNamesInTable(DataElementPath pathToTableWithData, String nameOfTableColumnWithSequences, String nameOfTableColumnWithSampleNames)
    {
        TableDataCollection table = pathToTableWithData.getDataElement(TableDataCollection.class);
        String[] sequenceSample = TableUtils.readGivenColumnInStringTable(table, nameOfTableColumnWithSequences);
        String[] sampleNames = TableUtils.readGivenColumnInStringTable(table, nameOfTableColumnWithSampleNames);
        return removeObjectsWithMissingData(sequenceSample, sampleNames);
    }
    
    private static Object[] removeObjectsWithMissingData(String[] sequenceSample, String[] sampleNames)
    {
        List<String> newSequenceSample = new ArrayList<>(), newSampleNames = new ArrayList<>();
        for( int i = 0; i < sequenceSample.length; i++ )
            if( sequenceSample[i] != null && ! sequenceSample[i].equals("") && sampleNames[i] != null && ! sampleNames[i].equals("") )
            {
                newSequenceSample.add(sequenceSample[i]);
                newSampleNames.add(sampleNames[i]);
            }
        if( newSequenceSample.isEmpty() ) return null;
        return new Object[]{newSequenceSample.toArray(new String[0]), newSampleNames.toArray(new String[0])};
    }

    private TableDataCollection writeTableWithOligFequenciesAndThierDiffernces(int oligLength, Map<String, List<byte[]>> sampleNameAndSequenceSample, DataElementPath pathToOutputs, String tableName)
    {
        List<String> columnNames = new ArrayList<>();
        List<double[]> transposedData = new ArrayList<>();
        for( Entry<String, List<byte[]>> entry : sampleNameAndSequenceSample.entrySet() )
        {
            columnNames.add(entry.getKey());
            List<byte[]> sequenceSample = entry.getValue();
            int[] oligCounts = Olig.getOligFrequencies(sequenceSample, oligLength);
            double[] oligFrequencies = new double[oligCounts.length];
            for( int i = 0; i < oligFrequencies.length; i++ )
                oligFrequencies[i] = (double)oligCounts[i] / sequenceSample.size();
            transposedData.add(oligFrequencies);
        }
        for( int i = 1; i < sampleNameAndSequenceSample.size(); i++ )
            for( int j = 0; j < i; j++ )
            {
                transposedData.add(MatrixUtils.getSubtractionOfVectors(transposedData.get(i), transposedData.get(j)));
                columnNames.add("Difference of olig frequencies: (" + columnNames.get(i) + ") - (" + columnNames.get(j) + ")");
            }
        String[] rowNames = new String[transposedData.get(0).length];
        for( int hash = 0; hash < transposedData.get(0).length; hash++ )
            rowNames[hash] = (new Olig(hash, oligLength)).toString();
        double[][] matrix = transposedData.toArray(new double[transposedData.size()][]);
        return TableUtils.writeDoubleTable(MatrixUtils.getTransposedMatrix(matrix), rowNames, columnNames.toArray(new String[0]), pathToOutputs, tableName);
    }
    
    public static class OligsInSequenceSamplesParameters extends AbstractMrnaAnalysisParameters
    {
        int oligLength = 6;
        DataElementPath pathToTableWithData;
        String nameOfTableColumnWithSequences;
        String nameOfTableColumnWithSampleNames;

        @PropertyName(MessageBundle.PN_OLIG_LENGTH)
        @PropertyDescription(MessageBundle.PD_OLIG_LENGTH)
        public int getOligLength()
        {
            return oligLength;
        }
        public void setOligLength(int oligLength)
        {
            Object oldValue = this.oligLength;
            this.oligLength = oligLength;
            firePropertyChange("oligLength", oldValue, oligLength);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_TABLE_WITH_DATA)
        @PropertyDescription(MessageBundle.PD_PATH_TO_TABLE_WITH_DATA)
        public DataElementPath getPathToTableWithData()
        {
            return pathToTableWithData;
        }
        public void setPathToTableWithData(DataElementPath pathToTableWithData)
        {
            Object oldValue = this.pathToTableWithData;
            this.pathToTableWithData= pathToTableWithData;
            firePropertyChange("pathToTableWithData", oldValue, pathToTableWithData);
        }
        
        @PropertyName(MessageBundle.PN_NAME_OF_TABLE_COLUMN_WITH_SEQUENCE_SAMPLE)
        @PropertyDescription(MessageBundle.PD_NAME_OF_TABLE_COLUMN_WITH_SEQUENCE_SAMPLE)
        public String getNameOfTableColumnWithSequences()
        {
            return nameOfTableColumnWithSequences;
        }
        public void setNameOfTableColumnWithSequences(String nameOfTableColumnWithSequences)
        {
            Object oldValue = this.nameOfTableColumnWithSequences;
            this.nameOfTableColumnWithSequences = nameOfTableColumnWithSequences;
            firePropertyChange("nameOfTableColumnWithSequences", oldValue, nameOfTableColumnWithSequences);
        }
        
        @PropertyName(MessageBundle.PN_NAME_OF_TABLE_COLUMN_SAMPLE_NAMES)
        @PropertyDescription(MessageBundle.PD_NAME_OF_TABLE_COLUMN_SAMPLE_NAMES)
        public String getNameOfTableColumnWithSampleNames()
        {
            return nameOfTableColumnWithSampleNames;
        }
        public void setNameOfTableColumnWithSampleNames(String nameOfTableColumnWithSampleNames)
        {
            Object oldValue = this.nameOfTableColumnWithSampleNames;
            this.nameOfTableColumnWithSampleNames = nameOfTableColumnWithSampleNames;
            firePropertyChange("nameOfTableColumnWithSampleNames", oldValue, nameOfTableColumnWithSampleNames);
        }
    }
    
    public static class OligsInSequenceSamplesParametersBeanInfo extends BeanInfoEx2<OligsInSequenceSamplesParameters>
    {
        public OligsInSequenceSamplesParametersBeanInfo()
        {
            super(OligsInSequenceSamplesParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add("oligLength");
            add(DataElementPathEditor.registerInput("pathToTableWithData", beanClass, TableDataCollection.class, false));
            
//          add(new PropertyDescriptorEx("nameOfTableColumnWithSequences", beanClass), TableColumnEditor.class);
//          add(new PropertyDescriptorEx("nameOfTableColumnWithSampleNames", beanClass), TableColumnEditor.class);
            add(ColumnNameSelector.registerSelector("nameOfTableColumnWithSequences", beanClass, "pathToTableWithData", false));
            add(ColumnNameSelector.registerSelector("nameOfTableColumnWithSampleNames", beanClass, "pathToTableWithData", false));
            
            add(DataElementPathEditor.registerOutput("pathToOutputTable", beanClass, TableDataCollection.class));
        }
    }
}

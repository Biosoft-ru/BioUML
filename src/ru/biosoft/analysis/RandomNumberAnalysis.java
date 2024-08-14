package ru.biosoft.analysis;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.RandomNumberAnalysisParameters.DistributionParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;

public class RandomNumberAnalysis extends AnalysisMethodSupport<RandomNumberAnalysisParameters>
{

    public RandomNumberAnalysis(DataCollection<?> parent, String name)
    {
        super(parent, name, new RandomNumberAnalysisParameters());
    }

    @Override
    public void setParameters(AnalysisParameters parameters)
    {
        if (parameters != null)
        this.parameters = (RandomNumberAnalysisParameters)parameters;
    }
  
    @Override
    public DataElement justAnalyzeAndPut() throws Exception
    {
        DistributionParameters[] numbers = {parameters.getRandomNumber1(), parameters.getRandomNumber2()};
        int length = numbers.length;
        DataElementPath output = parameters.getOutput();
        int[][] data = new int[length][parameters.getCount()];

        String[] columns = new String[length];
        boolean[] discrete = new boolean[length];

        for( int i = 0; i < length; i++ )
        {
            data[i] = numbers[i].draw(parameters.getCount());
            columns[i] = numbers[i].getVariableName();
            discrete[i] = numbers[i].isDiscrete();
        }
        data = Util.transpose(data);
        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection(output);

        for(int i=0; i<length; i++ )
            result.getColumnModel().addColumn(columns[i], discrete[i] ? DataType.Integer : DataType.Float);

        for( int i = 0; i < data.length; i++ )
            TableDataCollectionUtils.addRow(result, String.valueOf(i), ArrayUtils.toObject(data[i]));

        output.getParentCollection().put(result);
        return result;
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
    }

    @Override
    public Map<String, String> generateScripts(AnalysisParameters parameters)
    {
        return Collections.emptyMap();
    }
}
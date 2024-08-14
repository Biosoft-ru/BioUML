package ru.biosoft.analysis;

import java.util.stream.Stream;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.TableDataCollection;

public class RunAnalysisParameters extends AbstractAnalysisParameters
{
    private DataElementPath analysis;
    private DataElementPath parametersTable;    
    private String row;
    
    @PropertyName("Analysis")
    public DataElementPath getAnalysis()
    {
        return analysis;
    }
    public void setAnalysis(DataElementPath analysis)
    {
        this.analysis = analysis;
    }
    
    @PropertyName("Parameters")
    public DataElementPath getParametersTable()
    {
        return parametersTable;
    }
    public void setParametersTable(DataElementPath parametersTable)
    {
        this.parametersTable = parametersTable;
    }
    
    @PropertyName("Row")
    public String getRow()
    {
        return row;
    }
    public void setRow(String row)
    {
        this.row = row;
    }

    public Stream<String> getAvailableRows()
    {
        return parametersTable.getDataElement( TableDataCollection.class ).getNameList().stream();
    }
}

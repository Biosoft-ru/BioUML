package ru.biosoft.analysis;

import com.developmentontheedge.beans.editors.TagEditorSupport;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class FilterTableParameters extends AbstractAnalysisParameters
{
    public static final String[] MODES = new String[] {
        "Rows for which expression is true",
        "Rows with highest value of expression",
        "Rows with lowest value of expression"
    };
    
    private DataElementPath inputPath, outputPath;
    private String filterExpression;
    private int filteringMode = 0;
    private int valuesCount = 100;
    
    @PropertyName("Input table")
    @PropertyDescription("Table to filter")
    public DataElementPath getInputPath()
    {
        return inputPath;
    }
    
    public void setInputPath(DataElementPath inputPath)
    {
        Object oldValue = this.inputPath;
        this.inputPath = inputPath;
        firePropertyChange("inputPath", oldValue, this.inputPath);
    }
    
    @PropertyName("Output table")
    @PropertyDescription("Path to the filtered table")
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }
    
    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange("outputPath", oldValue, this.outputPath);
    }
    
    @PropertyName("Filtering expression")
    @PropertyDescription("Expression in JavaScript like 'ColumnName1 > 5 && ColumnName2 < 0'")
    public String getFilterExpression()
    {
        return filterExpression;
    }
    
    public void setFilterExpression(String filterExpression)
    {
        Object oldValue = this.filterExpression;
        this.filterExpression = filterExpression;
        firePropertyChange("filterExpression", oldValue, this.filterExpression);
    }

    public String getIcon()
    {
        return IconFactory.getIconId(getInputPath());
    }

    @PropertyName("Filtering mode")
    @PropertyDescription("Which rows to select")
    public int getFilteringMode()
    {
        return filteringMode;
    }

    public void setFilteringMode(int filteringMode)
    {
        Object oldValue = this.filteringMode;
        this.filteringMode = filteringMode;
        firePropertyChange("filteringMode", oldValue, filteringMode);
        firePropertyChange("*", null, null);
    }

    @PropertyName("Rows count")
    @PropertyDescription("Number of rows in result")
    public int getValuesCount()
    {
        return valuesCount;
    }

    public void setValuesCount(int valuesCount)
    {
        Object oldValue = this.valuesCount;
        this.valuesCount = valuesCount;
        firePropertyChange("valuesCount", oldValue, valuesCount);
    }
    
    public boolean isValuesCountHidden()
    {
        return getFilteringMode() == 0;
    }
    
    public static class ModeSelector extends TagEditorSupport
    {
        public ModeSelector()
        {
            super(MODES, 0);
        }
    }
}

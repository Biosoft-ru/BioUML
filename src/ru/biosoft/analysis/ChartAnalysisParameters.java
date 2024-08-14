package ru.biosoft.analysis;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@SuppressWarnings ( "serial" )
public class ChartAnalysisParameters extends AbstractAnalysisParameters
{

    public static final String ADVANCED_PALETTE = "Advanced colors";
    public static final String PASTEL_PALETTE = "Pastel colors";
    public static final String DEFAULT_PALETTE = "Default colors";

    private DataElementPath inputTable;
    @PropertyName ( "Input table" )
    public DataElementPath getInputTable()
    {
        return inputTable;
    }

    public void setInputTable(DataElementPath inputTable)
    {
        DataElementPath oldValue = this.inputTable;
        this.inputTable = inputTable;
        firePropertyChange( "inputTable", oldValue, inputTable );
    }

    private String column;
    @PropertyName ( "Category column" )
    public String getColumn()
    {
        return column;
    }
    public void setColumn(String column)
    {
        String oldValue = this.column;
        this.column = column;
        firePropertyChange( "column", oldValue, column );
    }

    private String labelsColumn;
    @PropertyName ( "Labels column" )
    public String getLabelsColumn()
    {
        return labelsColumn;
    }
    public void setLabelsColumn(String labelsColumn)
    {
        String oldValue = this.labelsColumn;
        this.labelsColumn = labelsColumn;
        firePropertyChange( "labelsColumn", oldValue, labelsColumn );
    }

    private int maxPieces = 10;
    @PropertyName("Max pieces")
    public int getMaxPieces()
    {
        return maxPieces;
    }
    public void setMaxPieces(int maxPieces)
    {
        this.maxPieces = maxPieces;
    }

    private boolean addRemaininig = false;
    @PropertyName("Add remaining elements as one category")
    public boolean isAddRemaininig()
    {
        return addRemaininig;
    }

    public void setAddRemaininig(boolean addRemaininig)
    {
        this.addRemaininig = addRemaininig;
    }

    private DataElementPath outputChart;
    @PropertyName("Output chart")
    public DataElementPath getOutputChart()
    {
        return outputChart;
    }
    public void setOutputChart(DataElementPath outputChart)
    {
        this.outputChart = outputChart;
    }

    private String paletteName = ADVANCED_PALETTE;
    @PropertyName("Color palette")
    public String getPaletteName()
    {
        return paletteName;
    }

    public void setPaletteName(String paletteName)
    {
        this.paletteName = paletteName;
    }
}
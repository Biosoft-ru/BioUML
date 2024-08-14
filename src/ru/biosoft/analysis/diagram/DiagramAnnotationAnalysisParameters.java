package ru.biosoft.analysis.diagram;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
public class DiagramAnnotationAnalysisParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputDiagram, table, outputDiagram;
    private String column = ColumnNameSelector.NONE_COLUMN;
    
    @PropertyName("Input diagram")
    @PropertyDescription("Diagram to annotate")
    public DataElementPath getInputDiagram()
    {
        return inputDiagram;
    }
    public void setInputDiagram(DataElementPath inputDiagram)
    {
        Object oldValue = this.inputDiagram;
        this.inputDiagram = inputDiagram;
        firePropertyChange( "inputDiagram", oldValue, inputDiagram );
    }
    
    @PropertyName("Annotation table")
    @PropertyDescription("Table with diagram ids and annotations")
    public DataElementPath getTable()
    {
        return table;
    }
    public void setTable(DataElementPath table)
    {
        Object oldValue = this.table;
        this.table = table;
        firePropertyChange( "table", oldValue, table );
        TableDataCollection tableElement = table == null ? null : table.optDataElement(TableDataCollection.class);
        if( oldValue == null || tableElement == null
                || ( !oldValue.equals( table ) && !tableElement.getColumnModel().hasColumn( getColumn() ) ) )
            setColumn(ColumnNameSelector.NONE_COLUMN);
    }
    
    @PropertyName("Annotation column")
    @PropertyDescription("Column with annotations")
    public String getColumn()
    {
        return column;
    }
    
    public void setColumn(String column)
    {
        Object oldValue = this.column;
        this.column = column;
        firePropertyChange( "column", oldValue, column );
    }

    @PropertyName("Output diagram")
    @PropertyDescription("Path to store annotated diagram")
    public DataElementPath getOutputDiagram()
    {
        return outputDiagram;
    }
    public void setOutputDiagram(DataElementPath outputDiagram)
    {
        Object oldValue = this.outputDiagram;
        this.outputDiagram = outputDiagram;
        firePropertyChange( "outputDiagram", oldValue, outputDiagram );
    }
}

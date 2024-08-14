package biouml.plugins.enrichment;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
@PropertyName("Enrichment analysis")
@PropertyDescription("Enrichment analysis")
public class EnrichmentAnalysisParameters extends FunctionalClassificationParameters
{
    private int permutationsCount;
    private String columnName = ColumnNameSelector.NONE_COLUMN;
    
    public EnrichmentAnalysisParameters()
    {
        super();
        permutationsCount = 1000;
    }
    
    @PropertyName("Number of permutations")
    @PropertyDescription("Number of random permutations used for p-value calculation (10-10000). Bigger values increase p-value precision, but make the analysis slower.")
    public int getPermutationsCount()
    {
        return permutationsCount;
    }

    public void setPermutationsCount(int permutationsCount)
    {
        Object oldValue = this.permutationsCount;
        this.permutationsCount = permutationsCount;
        firePropertyChange("permutationsCount", oldValue, this.permutationsCount);
    }

    @PropertyName("Weight column")
    @PropertyDescription("Column to rank genes by. Gene is considered top-ranked if value in this column is the highest.")
    public String getColumnName()
    {
        return columnName;
    }
    
    public void setColumnName(String columnName)
    {
        Object oldValue = this.columnName;
        this.columnName = columnName;
        firePropertyChange("columnName", oldValue, this.columnName);
    }
    
    @Override
    public void setSourcePath(DataElementPath path)
    {
        super.setSourcePath(path);
        if(getSource() != null)
        {
            ColumnModel columnModel = getSource().getColumnModel();
            if(columnModel.hasColumn(getColumnName())) return;
            for(TableColumn column: columnModel)
            {
                if(column.getType().isNumeric())
                {
                    setColumnName(column.getName());
                    return;
                }
            }
        }
        setColumnName(ColumnNameSelector.NONE_COLUMN);
    }
}

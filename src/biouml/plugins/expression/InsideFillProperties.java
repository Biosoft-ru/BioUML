package biouml.plugins.expression;

import java.util.DoubleSummaryStatistics;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.Pair;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
public class InsideFillProperties extends AbstractFillProperties
{
    private String column = ColumnNameSelector.NONE_COLUMN;
    @PropertyName("Column")
    @PropertyDescription("Column to create inside fill")
    public String getColumn()
    {
        return column;
    }

    public void setColumn(String column)
    {
        if(column != null && this.column != null && column.equals(this.column)) return;
        Object oldValue = this.column;
        this.column = column;
        firePropertyChange("column", oldValue, column);
        if( getLoading() )
            return;
        try
        {
            DoubleSummaryStatistics stats = TableDataCollectionUtils.findMinMax(getTable().getDataElement(TableDataCollection.class), new String[] {column});
            tableMinMax = new Pair<>(stats.getMin(), stats.getMax());
        }
        catch( Exception e )
        {
            tableMinMax = null;
        }
        if(tableMinMax == null || tableMinMax.getFirst()>tableMinMax.getSecond())
        {
            tableMinMax = new Pair<>(-1.0, 1.0);
        } else
        {
            correctMinMax();
        }
   }

    protected void correctMinMax()
    {
        double max = Math.max(Math.abs(tableMinMax.getFirst()), Math.abs(tableMinMax.getSecond()));
        tableMinMax.setFirst(-max);
        tableMinMax.setSecond(max);
    }
}
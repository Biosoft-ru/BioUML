package biouml.plugins.expression;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.Pair;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
public class OutsideFillProperties extends AbstractFillProperties
{
    private String[] columns;
    private int fillWidth = 20;
    private boolean useGradientFill = true;

    @PropertyName("Columns")
    @PropertyDescription("List of columns to create outside fill")
    public String[] getColumns()
    {
        return columns;
    }

    public void setColumns(String[] columns)
    {
        if(Arrays.equals(columns, this.columns)) return;
        Object oldValue = this.columns;
        this.columns = columns;
        firePropertyChange("columns", oldValue, columns);
        if( getLoading() )
            return;
        try
        {
            DoubleSummaryStatistics stats = TableDataCollectionUtils.findMinMax(getTable().getDataElement(TableDataCollection.class), getColumns());
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
            double max = Math.max(Math.abs(tableMinMax.getFirst()), Math.abs(tableMinMax.getSecond()));
            tableMinMax.setFirst(-max);
            tableMinMax.setSecond(max);
        }
    }

    @PropertyName("Width of the fill")
    @PropertyDescription("Range from 1 to 40")
    public int getFillWidth()
    {
        return fillWidth;
    }

    public void setFillWidth(int fillWidth)
    {
        if(fillWidth < 1) fillWidth = 1;
        if(fillWidth > 40) fillWidth = 40;
        Object oldValue = this.fillWidth;
        this.fillWidth = fillWidth;
        firePropertyChange("fillWidth", oldValue, fillWidth);
    }

    @PropertyName ( "Gradient fill" )
    @PropertyDescription ( "Use fading gradient aura" )
    public boolean isUseGradientFill()
    {
        return useGradientFill;
    }

    public void setUseGradientFill(boolean useGradientFill)
    {
        Object oldValue = this.useGradientFill;
        this.useGradientFill = useGradientFill;
        firePropertyChange( "useGradientFill", oldValue, useGradientFill );
    }

}
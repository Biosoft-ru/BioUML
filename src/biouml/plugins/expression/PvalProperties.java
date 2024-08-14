package biouml.plugins.expression;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.JSONBean;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import com.developmentontheedge.beans.Option;

/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public class PvalProperties extends Option implements JSONBean
{
    private String column = ColumnNameSelector.NONE_COLUMN;
    private double cutoff1 = 0.05, cutoff2 = 0.01, cutoff3 = 0.001;

    @PropertyName("Column")
    @PropertyDescription("Column with p-values")
    public String getColumn()
    {
        return column;
    }

    public void setColumn(String column)
    {
        Object oldValue = this.column;
        this.column = column;
        firePropertyChange("column", oldValue, column);
    }

    @PropertyName("* cutoff")
    @PropertyDescription("If value below this cutoff, one star is displayed")
    public double getCutoff1()
    {
        return cutoff1;
    }

    public void setCutoff1(double cutoff1)
    {
        Object oldValue = this.cutoff1;
        this.cutoff1 = cutoff1;
        firePropertyChange("cutoff1", oldValue, cutoff1);
    }

    @PropertyName("** cutoff")
    @PropertyDescription("If value below this cutoff, two stars are displayed")
    public double getCutoff2()
    {
        return cutoff2;
    }

    public void setCutoff2(double cutoff2)
    {
        Object oldValue = this.cutoff2;
        this.cutoff2 = cutoff2;
        firePropertyChange("cutoff2", oldValue, cutoff2);
    }

    @PropertyName("*** cutoff")
    @PropertyDescription("If value below this cutoff, three stars are displayed")
    public double getCutoff3()
    {
        return cutoff3;
    }

    public void setCutoff3(double cutoff3)
    {
        Object oldValue = this.cutoff3;
        this.cutoff3 = cutoff3;
        firePropertyChange("cutoff3", oldValue, cutoff3);
    }

    public DataElementPath getTable()
    {
        return ((ExpressionFilterProperties)getParent()).getTable();
    }
    
    public String getStars(double value)
    {
        if(value < cutoff3) return "***";
        if(value < cutoff2) return "**";
        if(value < cutoff1) return "*";
        return "";
    }
}

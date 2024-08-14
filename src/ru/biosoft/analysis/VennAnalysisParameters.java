package ru.biosoft.analysis;

import java.awt.Color;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class VennAnalysisParameters extends AbstractAnalysisParameters
{
    private static final int ALPHA_COLOR = 0x80;

    private DataElementPath table1Path, table2Path, table3Path;
    private DataElementPath output;
    private String table1Name, table2Name, table3Name;
    private boolean simple = true;
    
    private Color circle1Color = new Color(0xFF, 0xFF, 0x00, ALPHA_COLOR);
    private Color circle2Color = new Color(0xFF, 0x00, 0xFF, ALPHA_COLOR);
    private Color circle3Color = new Color(0x00, 0xFF, 0xFF, ALPHA_COLOR);

    public TableDataCollection getTable1()
    {
        try
        {
            return (TableDataCollection)table1Path.optDataElement();
        }
        catch( Exception ex )
        {
            return null;
        }
    }

    public TableDataCollection getTable2()
    {
        try
        {
            return (TableDataCollection)table2Path.optDataElement();
        }
        catch( Exception ex )
        {
            return null;
        }
    }
    
    public TableDataCollection getTable3()
    {
        try
        {
            return (TableDataCollection)table3Path.optDataElement();
        }
        catch( Exception ex )
        {
            return null;
        }
    }

    @PropertyName("Left table (T1)")
    @PropertyDescription("Table which will be represented as left-top circle")
    public DataElementPath getTable1Path()
    {
        return table1Path;
    }

    public void setTable1Path(DataElementPath table1Path)
    {
        Object oldValue = this.table1Path;
        this.table1Path = table1Path;
        firePropertyChange("table1Path", oldValue, table1Path);
    }

    @PropertyName("Right table (T2)")
    @PropertyDescription("Table which will be represented as right-top circle")
    public DataElementPath getTable2Path()
    {
        return table2Path;
    }

    public void setTable2Path(DataElementPath table2Path)
    {
        Object oldValue = this.table2Path;
        this.table2Path = table2Path;
        firePropertyChange("table2Path", oldValue, table2Path);
    }

    @PropertyName("Center table (T3)")
    @PropertyDescription("Table which will be represented as center-bottom circle")
    public DataElementPath getTable3Path()
    {
        return table3Path;
    }

    public void setTable3Path(DataElementPath table3Path)
    {
        Object oldValue = this.table3Path;
        this.table3Path = table3Path;
        firePropertyChange("table3Path", oldValue, table3Path);
    }

    @PropertyName("Simple picture")
    @PropertyDescription("All circles has equal radius")
    public boolean isSimple()
    {
        return simple;
    }

    public void setSimple(boolean simple)
    {
        Object oldValue = this.simple;
        this.simple = simple;
        firePropertyChange("simple", oldValue, simple);
    }

    @PropertyName("Output path")
    @PropertyDescription("Folder name to store the results")
    public DataElementPath getOutput()
    {
        return output;
    }

    public void setOutput(DataElementPath output)
    {
        Object oldValue = this.output;
        this.output = output;
        firePropertyChange("output", oldValue, output);
    }

    @PropertyName("Left table name")
    @PropertyDescription("Name for the left table on the diagram (leave empty to use table name)")
    public String getTable1Name()
    {
        return table1Name;
    }

    public void setTable1Name(String table1Name)
    {
        this.table1Name = table1Name;
    }

    @PropertyName("Right table name")
    @PropertyDescription("Name for the right table on the diagram (leave empty to use table name)")
    public String getTable2Name()
    {
        return table2Name;
    }

    public void setTable2Name(String table2Name)
    {
        this.table2Name = table2Name;
    }

    @PropertyName("Center table name")
    @PropertyDescription("Name for the center table on the diagram (leave empty to use table name)")
    public String getTable3Name()
    {
        return table3Name;
    }

    public void setTable3Name(String table3Name)
    {
        this.table3Name = table3Name;
    }

    @PropertyName("Left-top circle color")
    @PropertyDescription("Color for the left-top circle on the diagram")
    public Color getCircle1Color()
    {
        return circle1Color;
    }

    public void setCircle1Color(Color circle1Color)
    {
        this.circle1Color = new Color(circle1Color.getRed(), circle1Color.getGreen(), circle1Color.getBlue(), ALPHA_COLOR);
    }

    @PropertyName("Right-top circle color")
    @PropertyDescription("Color for the right-top circle on the diagram")
    public Color getCircle2Color()
    {
        return circle2Color;
    }

    public void setCircle2Color(Color circle2Color)
    {
        this.circle2Color = new Color(circle2Color.getRed(), circle2Color.getGreen(), circle2Color.getBlue(), ALPHA_COLOR);
    }

    @PropertyName("Center-bottom circle color")
    @PropertyDescription("Color for the center-bottom circle on the diagram")
    public Color getCircle3Color()
    {
        return circle3Color;
    }

    public void setCircle3Color(Color circle3Color)
    {
        this.circle3Color = new Color(circle3Color.getRed(), circle3Color.getGreen(), circle3Color.getBlue(), ALPHA_COLOR);
    }
}

package ru.biosoft.analysis;

import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.columnbeans.ColumnGroup;

public class SelectColumnsParameters extends AbstractAnalysisParameters
{
    private ColumnGroup columnGroup;
    private DataElementPath output;
    
    public SelectColumnsParameters()
    {
        output = null;
        columnGroup = new ColumnGroup(this);
    }

    public void setOutput(DataElementPath output)
    {
        this.output = output;
        firePropertyChange("*", null, null);
    }

    public DataElementPath getOutput()
    {
        return output;
    }

    public ColumnGroup getColumnGroup()
    {
        return columnGroup;
    }
    public void setColumnGroup(ColumnGroup group)
    {
        columnGroup = group;
        if(columnGroup != null)
            columnGroup.setParent(this);
        firePropertyChange("*", null, null);
    }

    
    @Override
    public void read(Properties properties, String prefix)
    {
        super.read(properties, prefix);
        String columnGroupStr = properties.getProperty(prefix + "columnGroup");
        if( columnGroupStr != null )
        {
            columnGroup = ColumnGroup.readObject(this, columnGroupStr);
        }
    }

    @Override
    public @Nonnull String[] getInputNames()
    {
        return new String[] {"columnGroup/tablePath"};
    }
}

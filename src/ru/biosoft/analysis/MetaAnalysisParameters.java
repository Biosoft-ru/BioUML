package ru.biosoft.analysis;

import ru.biosoft.access.core.DataElementPathSet;

public class MetaAnalysisParameters extends HypergeometricAnalysisParameters
{
    private DataElementPathSet tablePaths = new DataElementPathSet();

    public MetaAnalysisParameters()
    {
    }

    /**
     * @return the tablePaths
     */
    public DataElementPathSet getTablePaths()
    {
        return tablePaths;
    }

    /**
     * @param tablePaths the tablePaths to set
     */
    public void setTablePaths(DataElementPathSet tablePaths)
    {
        Object oldValue = this.tablePaths;
        this.tablePaths = tablePaths;
        firePropertyChange("tablePaths", oldValue, tablePaths);
    }
}
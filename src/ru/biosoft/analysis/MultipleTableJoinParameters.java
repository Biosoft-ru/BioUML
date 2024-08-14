
package ru.biosoft.analysis;

import java.util.Vector;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.analysis.aggregate.NumericAggregator;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@SuppressWarnings ( "serial" )
public class MultipleTableJoinParameters extends AbstractAnalysisParameters
{
    private DataElementPathSet tablePaths;
    private DataElementPath outputPath;
    protected Integer joinType;
    private final Vector<Integer> joinTypes = new Vector<>( 2 );
    private boolean mergeColumns = true;
    private boolean ignoreNaNInAggregator = true;
    private NumericAggregator aggregator;
    
    public MultipleTableJoinParameters()
    {
        setTablePaths(new DataElementPathSet());
        joinType = TableDataCollectionUtils.INNER_JOIN;

        joinTypes.add(TableDataCollectionUtils.INNER_JOIN);
        joinTypes.add(TableDataCollectionUtils.OUTER_JOIN);
        
        setAggregator(NumericAggregator.getAggregators()[0]);
    }
    
    
    public DataElementPathSet getTablePaths()
    {
        return tablePaths;
    }
    public void setTablePaths(DataElementPathSet tablePaths)
    {
        Object oldValue = this.tablePaths;
        this.tablePaths = tablePaths;
        firePropertyChange("tablePaths", oldValue, tablePaths);
    }
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }
    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange("outputPath", oldValue, outputPath);
    }
    public Integer getJoinType()
    {
        return joinTypes == null?0:joinTypes.indexOf(joinType);
    }
    public void setJoinType(Integer typeIndex)
    {
        Integer oldValue = joinType;
        joinType = joinTypes.get(typeIndex);
        firePropertyChange("joinType", oldValue, joinType);
    }
    public int getJoinTypeForAnalysis()
    {
        return joinType;
    }
    
    public String getIcon()
    {
        return IconFactory.getClassIconId(TableDataCollection.class);
    }
    
    public boolean isMergeColumns()
    {
        return mergeColumns;
    }

    public void setMergeColumns(boolean mergeColumns)
    {
        Object oldValue = this.mergeColumns;
        this.mergeColumns = mergeColumns;
        firePropertyChange("mergeColumns", oldValue, mergeColumns);
    }
    
    public boolean isIgnoreNaNInAggregator()
    {
        return ignoreNaNInAggregator;
    }
    public void setIgnoreNaNInAggregator(boolean ignoreNaNInAggregator)
    {
        boolean oldValue = this.ignoreNaNInAggregator;
        this.ignoreNaNInAggregator = ignoreNaNInAggregator;
        firePropertyChange( "ignoreNaNInAggregator", oldValue, ignoreNaNInAggregator );
        if( aggregator != null )
            aggregator.setIgnoreNaNs( ignoreNaNInAggregator );
    }

    public NumericAggregator getAggregator()
    {
        return aggregator;
    }
    
    public void setAggregator(NumericAggregator aggregator)
    {
        if( aggregator != null )
            aggregator.setIgnoreNaNs( ignoreNaNInAggregator );
        Object oldValue = this.aggregator;
        this.aggregator = aggregator;
        firePropertyChange("aggregator", oldValue, aggregator);
    }
}

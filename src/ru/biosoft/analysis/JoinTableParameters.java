package ru.biosoft.analysis;

import java.util.Properties;
import java.util.Vector;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.analysis.aggregate.NumericAggregator;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.ColumnGroup;

public class JoinTableParameters extends AbstractAnalysisParameters
{
    protected Integer joinType;
    private final Vector<Integer> joinTypes = new Vector<>( 5 );
    private ColumnGroup leftGroup;
    private ColumnGroup rightGroup;
    private DataElementPath output;
    private boolean mergeColumns = true;
    private boolean ignoreNaNInAggregator = true;
    private NumericAggregator aggregator;

    public void setOutput(DataElementPath output)
    {
        Object oldValue = this.output;
        this.output = output;
        firePropertyChange( "output", oldValue, output );
    }

    public DataElementPath getOutput()
    {
        return output;
    }

    public DataCollection getOutputCollection()
    {
        return output.optParentCollection();
    }

    public ColumnGroup getLeftGroup()
    {
        return leftGroup;
    }
    public void setLeftGroup(ColumnGroup group)
    {
        Object oldValue = leftGroup;
        leftGroup = group;
        if(leftGroup != null)
            leftGroup.setParent(this);
        firePropertyChange( "leftGroup", oldValue, group );
    }

    public ColumnGroup getRightGroup()
    {
        return rightGroup;
    }
    public void setRightGroup(ColumnGroup group)
    {
        Object oldValue = rightGroup;
        rightGroup = group;
        if(rightGroup != null)
            rightGroup.setParent(this);
        firePropertyChange( "rightGroup", oldValue, group );
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

    public JoinTableParameters()
    {
        output = null;

        joinType = TableDataCollectionUtils.INNER_JOIN;

        joinTypes.add(TableDataCollectionUtils.INNER_JOIN);
        joinTypes.add(TableDataCollectionUtils.OUTER_JOIN);
        joinTypes.add(TableDataCollectionUtils.LEFT_JOIN);
        joinTypes.add(TableDataCollectionUtils.RIGHT_JOIN);
        joinTypes.add(TableDataCollectionUtils.LEFT_SUBSTRACTION);
        joinTypes.add(TableDataCollectionUtils.RIGHT_SUBSTRACTION);
        joinTypes.add(TableDataCollectionUtils.SYMMETRIC_DIFFERENCE);

        leftGroup = new ColumnGroup(this);
        rightGroup = new ColumnGroup(this);
        
        setAggregator(NumericAggregator.getAggregators()[0]);
    }

    public String getJoinTypeDescription()
    {
        switch( joinType )
        {
            case TableDataCollectionUtils.INNER_JOIN:
                return "'inner'";
            case TableDataCollectionUtils.LEFT_JOIN:
                return "'left'";
            case TableDataCollectionUtils.RIGHT_JOIN:
                return "'right'";
            case TableDataCollectionUtils.OUTER_JOIN:
                return "'outer'";
            case TableDataCollectionUtils.LEFT_SUBSTRACTION:
                return "'left only'";
            case TableDataCollectionUtils.RIGHT_SUBSTRACTION:
                return "'right only'";
            case TableDataCollectionUtils.SYMMETRIC_DIFFERENCE:
                return "'symmetric difference'";
            default:
                return "!error!";
        }
    }

    @Override
    public @Nonnull String[] getInputNames()
    {
        return new String[] {"leftGroup/tablePath", "rightGroup/tablePath"};
    }

    @Override
    public @Nonnull String[] getOutputNames()
    {
        return new String[] {"output"};
    }

    public DataElement getLeft()
    {
        return leftGroup.getTable();
    }

    public void setLeft(DataElement table)
    {
        if( table instanceof TableDataCollection )
        {
            leftGroup.setTable((TableDataCollection)table);
        }
    }

    public DataElement getRight()
    {
        return rightGroup.getTable();
    }

    public void setRight(DataElement table)
    {
        if( table instanceof TableDataCollection )
        {
            rightGroup.setTable((TableDataCollection)table);
        }
    }
    
    public String getIcon()
    {
        return IconFactory.getIconId(leftGroup.getTablePath());
    }

    @Override
    public void read(Properties properties, String prefix)
    {
        super.read(properties, prefix);
        String leftGroupStr = properties.getProperty(prefix + "leftGroup");
        if( leftGroupStr != null )
        {
            leftGroup = ColumnGroup.readObject(this, leftGroupStr);
        }
        String rightGroupStr = properties.getProperty(prefix + "rightGroup");
        if( rightGroupStr != null )
        {
            rightGroup = ColumnGroup.readObject(this, rightGroupStr);
        }
    }
}

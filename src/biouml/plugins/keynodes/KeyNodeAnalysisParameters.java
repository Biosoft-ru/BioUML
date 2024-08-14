package biouml.plugins.keynodes;

import java.util.Properties;

import com.developmentontheedge.beans.Option;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.TargetOptions.CollectionRecord;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.util.bean.JSONBean;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.plugins.keynodes.graph.GraphDecoratorParameters;
import biouml.plugins.keynodes.graph.GraphDecoratorRegistry;

@SuppressWarnings ( "serial" )
public class KeyNodeAnalysisParameters extends BasicKeyNodeAnalysisParameters
{
    protected int maxRadius;
    protected String direction;
    protected DataElementPath outputTable;
    protected boolean isCalculatingFDR = false;
    protected double scoreCutoff;
    protected double FDRcutoff;
    protected double ZScoreCutoff;
    protected String weightColumn = ColumnNameSelector.NONE_COLUMN;
    protected boolean isoformFactor = true;
    protected double penalty;
    protected boolean isInputSizeLimited = true;
    protected int inputSizeLimit;
    protected GraphDecoratorEntry[] decorators;
    protected String relationSign = RELATION_INCREASE;

    public static final String RELATION_INCREASE = "Only increase and activation";
    public static final String RELATION_DECREASE = "Only decrease and supression";
    public static final String RELATION_BOTH = "All types of relations";


    private long seed = 0;

    public KeyNodeAnalysisParameters()
    {
        maxRadius = 2;
        FDRcutoff = 0.05;
        scoreCutoff = 0.2;
        ZScoreCutoff = 1;
        penalty = 0.1;
        direction = DirectionEditor.getAvailableValuesStatic(false)[0];
        inputSizeLimit = 1000;
    }

    public int getReverseDirection()
    {
        return getSearchDirection() == BioHub.DIRECTION_DOWN ? BioHub.DIRECTION_UP : BioHub.DIRECTION_DOWN;
    }

    @PropertyName ( "Output name" )
    @PropertyDescription ( "Output name." )
    public DataElementPath getOutputTable()
    {
        return outputTable;
    }

    public void setOutputTable(DataElementPath outputTable)
    {
        Object oldValue = this.outputTable;
        this.outputTable = outputTable;
        firePropertyChange("outputTable", oldValue, this.outputTable);
    }

    @PropertyName ( "Max radius" )
    @PropertyDescription ( "Maximal search radius" )
    public int getMaxRadius()
    {
        return maxRadius;
    }

    public void setMaxRadius(int radius)
    {
        Object oldValue = maxRadius;
        maxRadius = radius;
        firePropertyChange("maxRadius", oldValue, maxRadius);
    }

    public int getSearchDirection()
    {
        return direction.equals(DirectionEditor.UPSTREAM) ? BioHub.DIRECTION_UP : BioHub.DIRECTION_DOWN;
    }

    @PropertyName ( "Search direction" )
    @PropertyDescription ( "Direction to perform search in (either upstream, downstream reactions or both directions)" )
    public String getDirection()
    {
        return direction;
    }

    public void setDirection(String direction)
    {
        String oldValue = this.direction;
        this.direction = direction;
        firePropertyChange("direction", oldValue, this.direction);
    }

    @PropertyName ( "Calculate FDR" )
    @PropertyDescription ( "If true, analysis will calculate False Discovery Rate" )
    public boolean isCalculatingFDR()
    {
        return isCalculatingFDR;
    }

    public void setCalculatingFDR(boolean isCalculatingFDR)
    {
        boolean oldValue = this.isCalculatingFDR;
        this.isCalculatingFDR = isCalculatingFDR;
        firePropertyChange("isCalculatingFDR", oldValue, isCalculatingFDR);
        firePropertyChange("*", null, null);
    }

    @Override
    public void write(Properties properties, String prefix)
    {
        super.write(properties, prefix);
        properties.put(prefix + "plugin", "biouml.plugins.keynodes");
        properties.put(prefix + "visualizerName", "Network visualizer");
    }

    @PropertyName ( "Weighting column" )
    @PropertyDescription ( "Column to replace weights in search graph" )
    public String getWeightColumn()
    {
        return weightColumn;
    }

    public void setWeightColumn(String weightColumn)
    {
        Object oldValue = this.weightColumn;
        this.weightColumn = weightColumn;
        firePropertyChange("columnName", oldValue, this.weightColumn);
    }

    @PropertyName ( "Normalize multi-forms" )
    @PropertyDescription ( "Normalize weights of multiple forms" )
    public boolean isIsoformFactor()
    {
        return isoformFactor;
    }

    public void setIsoformFactor(boolean isoformFactor)
    {
        this.isoformFactor = isoformFactor;
    }

    @PropertyName ( "FDR cutoff" )
    @PropertyDescription ( "Molecules with FDR higher than specified will be excluded from the result" )
    public double getFDRcutoff()
    {
        return FDRcutoff;
    }

    public void setFDRcutoff(double fDRcutoff)
    {
        Object oldValue = FDRcutoff;
        FDRcutoff = fDRcutoff;
        firePropertyChange("FDRcutoff", oldValue, FDRcutoff);
    }

    public boolean isFDRParametersHidden()
    {
        return !isCalculatingFDR();
    }

    @PropertyName ( "Penalty" )
    @PropertyDescription ( "Penalty value for false positives" )
    public double getPenalty()
    {
        return penalty;
    }

    public void setPenalty(double penalty)
    {
        Object oldValue = this.penalty;
        this.penalty = penalty;
        firePropertyChange("penalty", oldValue, penalty);
    }

    @PropertyName ( "Limit input size" )
    @PropertyDescription ( "Limit size of input list" )
    public boolean isInputSizeLimited()
    {
        return isInputSizeLimited;
    }

    public void setInputSizeLimited(boolean isInputSizeLimited)
    {
        Object oldValue = this.isInputSizeLimited;
        this.isInputSizeLimited = isInputSizeLimited;
        firePropertyChange("isInputSizeLimited", oldValue, isInputSizeLimited);
    }

    @PropertyName ( "Input size" )
    @PropertyDescription ( "Size of input list" )
    public int getInputSizeLimit()
    {
        return inputSizeLimit;
    }

    public void setInputSizeLimit(int inputSizeLimit)
    {
        Object oldValue = this.inputSizeLimit;
        this.inputSizeLimit = inputSizeLimit;
        firePropertyChange("inputSizeLimit", oldValue, inputSizeLimit);
    }

    public boolean isDirectionBothAvailable()
    {
        return false;
    }

    public String getIcon()
    {
        try
        {
            return getKeyNodesHub().getSupportedInputTypes()[0].getIconId();
        }
        catch( Exception e )
        {
            return null;
        }
    }

    @PropertyName ( "Score cutoff" )
    @PropertyDescription ( "Molecules with Score lower than specified will be excluded from the result" )
    public double getScoreCutoff()
    {
        return scoreCutoff;
    }

    public void setScoreCutoff(double scoreCutoff)
    {
        Object oldValue = this.scoreCutoff;
        this.scoreCutoff = scoreCutoff;
        firePropertyChange("scoreCutoff", oldValue, scoreCutoff);
    }

    @PropertyName ( "Z-score cutoff" )
    @PropertyDescription ( "Molecules with Z-score lower than specified will be excluded from the result" )
    public double getZScoreCutoff()
    {
        return ZScoreCutoff;
    }

    public void setZScoreCutoff(double zScoreCutoff)
    {
        Object oldValue = ZScoreCutoff;
        ZScoreCutoff = zScoreCutoff;
        firePropertyChange("zScoreCutoff", oldValue, zScoreCutoff);
    }

    @Override
    public void setSourcePath(DataElementPath sourcePath)
    {
        super.setSourcePath(sourcePath);
        if( getSource() != null )
            setWeightColumn(ColumnNameSelector.NONE_COLUMN);
        else
            setWeightColumn("");
    }

    @PropertyName("Decorators")
    @PropertyDescription ( "Possibility to modify search collection with custom interaction sets" )
    public GraphDecoratorEntry[] getDecorators()
    {
        return decorators == null ? new GraphDecoratorEntry[0] : decorators;
    }

    public void setDecorators(GraphDecoratorEntry[] decorators)
    {
        Object oldValue = this.decorators;
        this.decorators = decorators;
        for(GraphDecoratorEntry decorator : decorators)
        {
            decorator.setParent( this );
            decorator.getParameters().setKeyNodeParameters( this );
        }
        firePropertyChange( "decorators", oldValue, this.decorators );
    }

    public static class GraphDecoratorEntry extends Option implements JSONBean
    {
        private String decoratorName = "";
        private GraphDecoratorParameters parameters;

        public GraphDecoratorEntry()
        {
            setDecoratorName( "None" );
        }

        public CollectionRecord createCollectionRecord()
        {
            return GraphDecoratorRegistry.createCollectionRecord( decoratorName, parameters );
        }

        @PropertyName("Decorator name")
        public String getDecoratorName()
        {
            return decoratorName;
        }

        public void setDecoratorName(String decoratorName)
        {
            if(!this.decoratorName.equals( decoratorName ))
            {
                Object oldValue = this.decoratorName;
                this.decoratorName = TextUtil.nullToEmpty( decoratorName );
                firePropertyChange( "decoratorName", oldValue, this.decoratorName );
                setParameters( GraphDecoratorRegistry.createParameters( decoratorName ) );
            }
        }

        @PropertyName("Decorator parameters")
        public GraphDecoratorParameters getParameters()
        {
            return parameters;
        }
        public void setParameters(GraphDecoratorParameters parameters)
        {
            Object oldValue = this.parameters;
            this.parameters = parameters;
            firePropertyChange( "parameters", oldValue, this.parameters );
        }

        public boolean isAcceptable(KeyNodesHub<?> bioHub)
        {
            return GraphDecoratorRegistry.getDecorator(decoratorName).isAcceptable(bioHub);
        }

        @Override
        public String toString()
        {
            return getDecoratorName();
        }
    }

    public static class GraphDecoratorEntryBeanInfo extends BeanInfoEx2<GraphDecoratorEntry>
    {
        public GraphDecoratorEntryBeanInfo()
        {
            super(GraphDecoratorEntry.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "decoratorName" )
                    .tags( bean -> GraphDecoratorRegistry.decorators( ( (KeyNodeAnalysisParameters)bean.getParent() ).getKeyNodesHub() ) )
                    .structureChanging().add();
            add( "parameters" );
        }
    }

    @PropertyName ( "Random seed" )
    @PropertyDescription ( "Seed for FDR random generator" )
    public long getSeed()
    {
        return seed;
    }
    public void setSeed(long seed)
    {
        Object oldValue = this.seed;
        this.seed = seed;
        firePropertyChange( "seed", oldValue, this.seed );
    }


    @PropertyName ( "Relation sign" )
    @PropertyDescription ( "Consider only specified type of relation chain between molecules." )
    public String getRelationSign()
    {
        return relationSign;
    }

    public void setRelationSign(String relationSign)
    {
        Object oldValue = this.relationSign;
        this.relationSign = relationSign;
        firePropertyChange( "relationSign", oldValue, this.relationSign );
    }

    public boolean isRelationSignHidden()
    {
        KeyNodesHub<?> hub = getKeyNodesHub();
        return !hub.isRelationSignSupported();
    }

}

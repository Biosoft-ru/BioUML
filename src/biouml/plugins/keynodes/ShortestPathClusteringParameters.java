package biouml.plugins.keynodes;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHub;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author anna
 */
@SuppressWarnings ( "serial" )
@PropertyName ( "Parameters" )
public class ShortestPathClusteringParameters extends BasicKeyNodeAnalysisParameters
{
    private DataElementPath outputPath;
    private int maxRadius;
    private String direction;
    private boolean useFullPath;
    private int inputSizeLimit = 300;

    public ShortestPathClusteringParameters()
    {
        direction = DirectionEditor.getAvailableValuesStatic(true)[0];
        maxRadius = 3;
    }

    @PropertyName ( "Output name" )
    @PropertyDescription ( "Output name." )
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

    @PropertyName ( "Max radius" )
    @PropertyDescription ( "Maximal search radius" )
    public int getMaxRadius()
    {
        return maxRadius;
    }

    public void setMaxRadius(int maxRadius)
    {
        Object oldValue = this.maxRadius;
        this.maxRadius = maxRadius;
        firePropertyChange("maxRadius", oldValue, maxRadius);
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

    public int getSearchDirection()
    {
        return direction.equals( DirectionEditor.UPSTREAM ) ? BioHub.DIRECTION_UP
                : direction.equals( DirectionEditor.DOWNSTREAM ) ? BioHub.DIRECTION_DOWN : BioHub.DIRECTION_BOTH;
    }

    public boolean isDirectionBothAvailable()
    {
        return true;
    }

    @PropertyName ( "Display intermediate molecules" )
    @PropertyDescription ( "Output the diagram with the direct reactions and all intermediate molecules" )
    public boolean isUseFullPath()
    {
        return useFullPath;
    }

    public void setUseFullPath(boolean useFullPath)
    {
        Object oldValue = this.useFullPath;
        this.useFullPath = useFullPath;
        firePropertyChange("useFullPath", oldValue, useFullPath);
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
}

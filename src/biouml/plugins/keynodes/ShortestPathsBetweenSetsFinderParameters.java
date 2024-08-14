package biouml.plugins.keynodes;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.table.TableDataCollection;

@SuppressWarnings ( "serial" )
@PropertyName ( "Parameters" )
@PropertyDescription ( "Set to set paths finder parameters" )
public class ShortestPathsBetweenSetsFinderParameters extends BasicKeyNodeAnalysisParameters
{
    private DataElementPath endSet;
    private String direction;
    private int maxRadius;
    private DataElementPath outputPath;

    public ShortestPathsBetweenSetsFinderParameters()
    {
        direction = DirectionEditor.getAvailableValuesStatic( true )[0];
        maxRadius = 3;
    }

    public TableDataCollection getTarget()
    {
        return ( endSet == null || ! ( endSet.optDataElement() instanceof TableDataCollection ) ) ? null
                : (TableDataCollection)endSet.optDataElement();
    }

    @Override
    @PropertyName ( "Start set" )
    @PropertyDescription ( "Set with elements to start path with" )
    public DataElementPath getSourcePath()
    {
        return super.getSourcePath();
    }

    @PropertyName ( "End set" )
    @PropertyDescription ( "Set with elements to end path with" )
    public DataElementPath getEndSet()
    {
        return endSet;
    }
    public void setEndSet(DataElementPath endSet)
    {
        Object oldValue = this.endSet;
        this.endSet = endSet;
        firePropertyChange( "endSet", oldValue, endSet );
    }

    @PropertyName ( "Search direction" )
    @PropertyDescription ( "Direction to perform search in (either upstream, downstream reactions or both directions)" )
    public String getDirection()
    {
        return direction;
    }
    public void setDirection(String direction)
    {
        Object oldValue = this.direction;
        this.direction = direction;
        firePropertyChange( "direction", oldValue, direction );
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
        firePropertyChange( "maxRadius", oldValue, maxRadius );
    }

    @PropertyName ( "Output collection" )
    @PropertyDescription ( "Path to table with results" )
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }
    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange( "outputPath", oldValue, outputPath );
    }

    public boolean isDirectionBothAvailable()
    {
        return true;
    }
    public int getSearchDirection()
    {
        return direction.equals( DirectionEditor.UPSTREAM ) ? BioHub.DIRECTION_UP
                : direction.equals( DirectionEditor.DOWNSTREAM ) ? BioHub.DIRECTION_DOWN : BioHub.DIRECTION_BOTH;
    }
    public int getReverseDirection()
    {
        int searchDirection = getSearchDirection();
        return searchDirection == BioHub.DIRECTION_BOTH ? BioHub.DIRECTION_BOTH
                : ( searchDirection == BioHub.DIRECTION_DOWN ? BioHub.DIRECTION_UP : BioHub.DIRECTION_DOWN );
    }
}

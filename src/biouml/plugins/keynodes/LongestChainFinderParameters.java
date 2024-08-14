package biouml.plugins.keynodes;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHub;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
public class LongestChainFinderParameters extends KeyNodeAnalysisParameters
{
    protected int maxDijkstraDepth;
    protected double scoreCoeff;

    public LongestChainFinderParameters()
    {
        inputSizeLimit = 1000;
        maxRadius = 10;
        maxDijkstraDepth = 100;
        scoreCoeff = 5;
        direction = DirectionEditor.DOWNSTREAM;
    }

    @Override
    @PropertyName ( "Output table" )
    @PropertyDescription ( "Path to the result table" )
    public DataElementPath getOutputTable()
    {
        return super.getOutputTable();
    }

    @Override
    public int getSearchDirection()
    {
        return direction.equals( DirectionEditor.UPSTREAM ) ? BioHub.DIRECTION_UP : direction.equals( DirectionEditor.DOWNSTREAM )
                ? BioHub.DIRECTION_DOWN : BioHub.DIRECTION_BOTH;
    }

    @Override
    public int getReverseDirection()
    {
        int searchDirection = getSearchDirection();
        return searchDirection == BioHub.DIRECTION_BOTH ? BioHub.DIRECTION_BOTH : ( searchDirection == BioHub.DIRECTION_DOWN
                ? BioHub.DIRECTION_UP : BioHub.DIRECTION_DOWN );
    }

    @Override
    public boolean isDirectionBothAvailable()
    {
        return true;
    }

    @PropertyName ( "Max depth for Dijkstra" )
    @PropertyDescription ( "Maximal depth which will be used in Dijkstra search algorithm" )
    public int getMaxDijkstraDepth()
    {
        return maxDijkstraDepth;
    }
    public void setMaxDijkstraDepth(int maxDijkstraDepth)
    {
        Object oldValue = this.maxDijkstraDepth;
        this.maxDijkstraDepth = maxDijkstraDepth;
        firePropertyChange( "maxDijkstraDepth", oldValue, this.maxDijkstraDepth );
    }

    @PropertyName ( "Score coefficient" )
    @PropertyDescription ( "Parameter which is used to evalueate score" )
    public double getScoreCoeff()
    {
        return scoreCoeff;
    }
    public void setScoreCoeff(double scoreCoeff)
    {
        Object oldValue = this.scoreCoeff;
        this.scoreCoeff = scoreCoeff;
        firePropertyChange( "scoreCoeff", oldValue, this.scoreCoeff );
    }

}

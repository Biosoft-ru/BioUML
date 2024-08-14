package biouml.plugins.riboseq.coverageChecker;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import one.util.streamex.StreamEx;

public class CenterPointCollection
{
    private final Map<String, Map<Integer, CenterPoint>> chrCenterPointMap = new HashMap<>();

    public void put(String chrName, CenterPoint point)
    {
        final Map<Integer, CenterPoint> pointMap;
        if( chrCenterPointMap.containsKey( chrName ) )
        {
            pointMap = chrCenterPointMap.get( chrName );
            updatePointValue( pointMap, point );
        }
        else
        {
            pointMap = new TreeMap<>();
            final int pointKey = point.getPoint();
            pointMap.put( pointKey, point );

            chrCenterPointMap.put( chrName, pointMap );
        }
    }

    private void updatePointValue(Map<Integer, CenterPoint> pointMap, CenterPoint point)
    {
        final int pointKey = point.getPoint();
        final CenterPoint samePoint = pointMap.get( pointKey );
        if( samePoint != null )
        {
            samePoint.incrementCounter();
        }
        else
        {
            pointMap.put( pointKey, point );
        }
    }

    public List<CenterPoint> getChrPointList(String chrName)
    {
        return StreamEx.ofValues( chrCenterPointMap.get( chrName ) ).sorted( Comparator.comparingInt( CenterPoint::getPoint ) ).toList();
    }
}
package biouml.plugins.hemodynamics;

public class TreeIntegrityValidator
{
    int n;
    double tolerance = 1E-3;

    public TreeIntegrityValidator(ArterialBinaryTreeModel atm)
    {
        n = atm.root.getPressure().length;

        for( SimpleVessel vessel : atm.vessels )
        {
            checkPressure( vessel );
            checkFlow( vessel );
        }
    }

    public double getFullPressure(SimpleVessel v, int i)
    {
        double[] area = v.getArea();
        double[] flow = v.getFlow();
        double[] pressure = v.getPressure();
        return pressure[i] + 0.5 * ( flow[i] * flow[i] / ( area[i] * area[i] ) )/1333.223684;
    }
    
    public void checkPressure(SimpleVessel v)
    {
        if (v.left == null || v.right == null)
            return;
        
        double fullPressure1 = getFullPressure(v, n-1);
        double fullPressure2 = getFullPressure(v.left, 0);
        double fullPressure3 = getFullPressure(v.right, 0 );
        
        double error1 = fullPressure1 - fullPressure2;
        double error2 = fullPressure1 - fullPressure3;
        
        if (error1 > tolerance || error2 > tolerance)
            System.out.println( "Errors in vessel full pressure, vessel: "+v.title+" with left: "+error1+" with right: "+error2 );
        
    }
    
    public void checkFlow(SimpleVessel v)
    {
        if (v.left == null || v.right == null)
            return;
        
        double error = v.getFlow()[n-1] - v.right.getFlow()[0]-v.left.getFlow()[0];
              
        if (error > tolerance)
            System.out.println( "Errors in vessel flow, vessel: "+v.title+", error: "+error);
        
    }
    
    private double calculateReflection(SimpleVessel v, int vesselSegments)
    {
        double y = v.area[vesselSegments] / v.pulseWave[vesselSegments];
        if( v.left != null && v.right != null )
        {
            double y1 = v.left.area[vesselSegments] / v.left.pulseWave[vesselSegments];
            double y2 = v.right.area[vesselSegments] / v.right.pulseWave[vesselSegments];
            return ( y - y1 - y2 ) / ( y + y1 + y2 );
        }
        else if (v.left != null)
        {
            double y1 = v.left.area[vesselSegments] / v.left.pulseWave[vesselSegments];
            return ( y - y1 ) / ( y + y1  );
        }
        else  if (v.right != null)
        {
            double y1 = v.right.area[vesselSegments] / v.right.pulseWave[vesselSegments];
            return ( y - y1 ) / ( y + y1  );
        }
        else
        {
            return 1;
        }
    }
}

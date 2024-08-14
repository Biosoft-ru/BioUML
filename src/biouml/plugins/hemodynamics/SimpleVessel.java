package biouml.plugins.hemodynamics;

public class SimpleVessel
{
    public int depth = 0; // depth of the vessel's UP junction

    public SimpleVessel left = null; // vessel's child connected to the DOWN junction

    public SimpleVessel right = null; // vessel's child connected to the DOWN junction

    public SimpleVessel parent = null; // vessel parent

    public int index = 0;

    public double length;

    public double unweightedArea; // area
    public double unweightedArea1; // area
    public double[] area0;
    
    public double beta;

    public double resistance;

    public double[] area;

    public double[] flow;

    public double[] pressure;
    public double[] fullPressure;
    public double[] velocity;

    public double[] pulseWave;
    public double[] impedance;

    public String title;
    public String name;
    public SimpleVessel(String name, String title, double length, double area, double area1, double beta)
    {
        this.name = name;
        this.length = length;
        this.unweightedArea = area;
        this.unweightedArea1 = area1;
        this.beta = beta;
        this.title = title;
        //        this.resistance = 8*0.035*length*Math.PI/(area*area);
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void setArea(double[] A)
    {
        if( depth % 2 == 0 )
            area = A.clone();
        else
            area = reverse(A.clone());

    }

    public void setFlow(double[] Q)
    {
        if( depth % 2 == 0 )
            flow = Q.clone();//Q.getColumnPackedCopy();
        else
            flow = reverse(Q.clone());//Q.getColumnPackedCopy());
    }

    public void setPressure(double[] P)
    {
        if( depth % 2 == 0 )
            pressure = P.clone();//.getColumnPackedCopy();
        else
            pressure = reverse(P.clone());//.getColumnPackedCopy());
    }

    public void setVelocity(double[] P)
    {
        if( depth % 2 == 0 )
            velocity = P.clone();//.getColumnPackedCopy();
        else
            velocity = reverse(P.clone());//.getColumnPackedCopy());
    }


    public void setFullPressure(double[] P)
    {
        if( depth % 2 == 0 )
            fullPressure = P.clone();//.getColumnPackedCopy();
        else
            fullPressure = reverse(P.clone());//.getColumnPackedCopy());
    }

    public void setPulseWave(double[] P)
    {
        if( depth % 2 == 0 )
            pulseWave = P.clone();//.getColumnPackedCopy();
        else
            pulseWave = reverse(P.clone());//.getColumnPackedCopy());
    }

    public void setImpedance(double[] P)
    {
        if( depth % 2 == 0 )
            impedance = P.clone();//.getColumnPackedCopy();
        else
            impedance = reverse(P.clone());//.getColumnPackedCopy());
    }

    public double[] getArea()
    {
        return area;
    }

    public double[] getFlow()
    {
        return flow;
    }

    public double[] getPressure()
    {
        return pressure;
    }

    private double[] reverse(double[] a)
    {
        double[] result = new double[a.length];
        for( int i = 0; i < a.length; i++ )
        {
            int m = a.length - i - 1;
            result[i] = a[m];
        }
        return result;
    }
}

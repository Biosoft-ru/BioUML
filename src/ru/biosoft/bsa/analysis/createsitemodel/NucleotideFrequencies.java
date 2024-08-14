package ru.biosoft.bsa.analysis.createsitemodel;

public class NucleotideFrequencies
{

    private Double a = 0.25;
    private Double c = 0.25;
    private Double g = 0.25;
    private Double t = 0.25;
    
    public Double getA()
    {
        return a;
    }
    public void setA(Double a)
    {
        this.a = a;
    }
    
    public Double getC()
    {
        return c;
    }
    public void setC(Double c)
    {
        this.c = c;
    }
    
    public Double getG()
    {
        return g;
    }
    public void setG(Double g)
    {
        this.g = g;
    }
    
    public Double getT()
    {
        return t;
    }
    public void setT(Double t)
    {
        this.t = t;
    }
    
    public double[] toArray()
    {
        return new double[] {a, c, g, t};
    }
    
}


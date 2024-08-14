package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.physicell.core.Geometry;

@PropertyName ( "Geometry" )
public class GeometryProperties
{
    private Geometry g = new Geometry();

    public GeometryProperties()
    {
    }

    public GeometryProperties(Geometry g)
    {
        this.g = g;
    }

    public GeometryProperties clone()
    {
        return new GeometryProperties( g.clone() );
    }

    @PropertyName ( "Radius" )
    public double getRadius()
    {
        return g.radius;
    }
    public void setRadius(double radius)
    {
        g.radius = radius;
    }

    @PropertyName ( "Nuclear Radius" )
    public double getNuclearRadius()
    {
        return g.getNuclearRadius();
    }
    public void setNuclearRadius(double nuclearRadius)
    {
    }

    //    @PropertyName ( "Surface Area" )
    //    public double getSurfaceArea()
    //    {
    ////        return g.;
    //    }
    //    public void setSurfaceArea(double surfaceArea)
    //    {
    //    }

    @PropertyName ( "Polarity" )
    public double getPolarity()
    {
        return g.polarity;
    }
    public void setPolarity(double polarity)
    {
        g.polarity = polarity;
    }
}
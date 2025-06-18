package biouml.plugins.physicell.plot;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.physicell.MulticellEModel;

public class PlotProperties
{
   private MulticellEModel model;
    private CurveProperties[] properties = new CurveProperties[0];

    public PlotProperties()
    {
        
    }
    public PlotProperties(MulticellEModel model)
    {
        this.model = model;
    }
    
    public void setModel(MulticellEModel model)
    {
        this.model = model;
        for( CurveProperties prop : properties )
            prop.setModel( model );
    }
    
    @PropertyName("Curves")
    public CurveProperties[] getProperties()
    {
        return properties;
    }

    public void setProperties(CurveProperties[] properties)
    {
        this.properties = properties;
        for( CurveProperties prop : properties )
            prop.setModel( model );
    }
}

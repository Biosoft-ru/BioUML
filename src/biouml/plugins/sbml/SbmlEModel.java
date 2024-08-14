package biouml.plugins.sbml;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.DiagramElement;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.standard.type.Unit;

public class SbmlEModel extends EModel
{    
    public SbmlEModel(DiagramElement diagramElement)
    {
        super(diagramElement);
    }
    
    @Override
    public Role clone(DiagramElement de)
    {
        SbmlEModel emodel = new SbmlEModel(de);
        doClone(emodel);
        return emodel;
    }
    
    private String substanceUnits = Unit.UNDEFINED;
    private String timeUnits = Unit.UNDEFINED;
    private String volumeUnits = Unit.UNDEFINED;
    private String areaUnits = Unit.UNDEFINED;
    private String lengthUnits = Unit.UNDEFINED;
    private String extentUnits = Unit.UNDEFINED; 

    @PropertyName("Substance units")
    @PropertyDescription("Substance units.")
    public String getSubstanceUnits()
    {
        return substanceUnits;
    }

    public void setSubstanceUnits(String substanceUnits)
    {
        this.substanceUnits = substanceUnits;
    }

    @PropertyName("Time units")
    @PropertyDescription("Time units.")
    public String getTimeUnits()
    {
        return timeUnits;
    }

    public void setTimeUnits(String timeUnits)
    {
        this.timeUnits = timeUnits;
    }

    @PropertyName("Volume units")
    @PropertyDescription("Volume units.")
    public String getVolumeUnits()
    {
        return volumeUnits;
    }

    public void setVolumeUnits(String volumeUnits)
    {
        this.volumeUnits = volumeUnits;
    }

    @PropertyName("Area units")
    @PropertyDescription("Area units.")
    public String getAreaUnits()
    {
        return areaUnits;
    }

    public void setAreaUnits(String areaUnits)
    {
        this.areaUnits = areaUnits;
    }

    @PropertyName("Length units")
    @PropertyDescription("Length units.")
    public String getLengthUnits()
    {
        return lengthUnits;
    }

    public void setLengthUnits(String lengthUnits)
    {
        this.lengthUnits = lengthUnits;
    }

    @PropertyName("Extent units")
    @PropertyDescription("Extent units.")
    public String getExtentUnits()
    {
        return extentUnits;
    }

    public void setExtentUnits(String extentUnits)
    {
        this.extentUnits = extentUnits;
    }
}

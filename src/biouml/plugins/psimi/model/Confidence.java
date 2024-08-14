package biouml.plugins.psimi.model;

import biouml.standard.type.Concept;
import ru.biosoft.access.core.DataCollection;

@SuppressWarnings ( "serial" )
public class Confidence extends Concept
{
    public Confidence(DataCollection<?> origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_CONCEPT;
    }
    
    private Concept unit;
    private String value;
    private Integer[] experimentRefList;
    
    public Integer[] getExperimentRefList()
    {
        return experimentRefList;
    }

    public void setExperimentRefList(Integer[] experimentRefList)
    {
        this.experimentRefList = experimentRefList;
    }

    public Concept getUnit()
    {
        return unit;
    }

    public void setUnit(Concept unit)
    {
        this.unit = unit;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }
    
    
}

package ru.biosoft.bsa.track.combined;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.util.bean.JSONBean;

@SuppressWarnings ( "serial" )
public class CombineCondition extends Option implements JSONBean
{
    public static final String[] CONDITION_TYPES = {"union", "intersection"};

    private String conditionType = "union";
    private String formula = null;
    private int distance = 0;

    @PropertyName ( "Type" )
    @PropertyDescription ( "How to manage sites from various tracks" )
    public String getConditionType()
    {
        return conditionType;
    }
    public void setConditionType(String conditionType)
    {
        Object oldValue = this.conditionType;
        this.conditionType = conditionType;
        firePropertyChange( "conditionType", oldValue, conditionType );
    }

    @PropertyName ( "Formula" )
    @PropertyDescription ( "Define the rule of site group selection" )
    public String getFormula()
    {
        return formula;
    }
    public void setFormula(String formula)
    {
        Object oldValue = this.formula;
        this.formula = formula;
        firePropertyChange( "formula", oldValue, formula );
    }

    @PropertyName ( "Sites distance" )
    @PropertyDescription ( "Minimal distance between sites to put them to separate groups" )
    public int getDistance()
    {
        return distance;
    }
    public void setDistance(int distance)
    {
        Object oldValue = this.distance;
        this.distance = distance;
        firePropertyChange( "distance", oldValue, distance );
    }
}

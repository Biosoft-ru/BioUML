package biouml.standard.type;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName ( "Kinetic law" )
@PropertyDescription("Kinetic law describes the reaction kinetics.")
public class KineticLaw extends Option
{
    protected String formula = "0";
    protected String timeUnits;
    protected String substanceUnits;
    protected String comment;
    
    public KineticLaw()
    {}

    public KineticLaw(String formula)
    {
        setFormula(formula);
    }
    
    public KineticLaw(Reaction parent)
    {
        this.setParent(parent);
    }

    public KineticLaw(Reaction parent, KineticLaw original)
    {
        this.setParent(parent);
        this.setComment(original.getComment());
        this.setFormula(original.getFormula());
        this.setSubstanceUnits(original.getSubstanceUnits());
        this.setTimeUnits(original.getTimeUnits());
    }
    
    @PropertyName("Formula")
    @PropertyDescription("Reaction rate formula.")
    public String getFormula()
    {
        return formula;
    }
    public void setFormula(String formula)
    {
        String oldValue = this.formula;
        this.formula = formula;
        firePropertyChange("formula", oldValue, formula);
    }

    @PropertyName("Time units")
    @PropertyDescription("Reaction rate time units.")
    public String getTimeUnits()
    {
        return timeUnits;
    }
    public void setTimeUnits(String timeUnits)
    {
        String oldValue = this.timeUnits;
        this.timeUnits = timeUnits;
        firePropertyChange("timeUnits", oldValue, timeUnits);
    }

    @PropertyName("Substance units")
    @PropertyDescription("Reaction rate substance units.")
    public String getSubstanceUnits()
    {
        return substanceUnits;
    }
    public void setSubstanceUnits(String substanceUnits)
    {
        String oldValue = this.substanceUnits;
        this.substanceUnits = substanceUnits;
        firePropertyChange("substanceUnits", oldValue, substanceUnits);
    }

    @PropertyName("Comment")
    @PropertyDescription("Comment on reaction rate.")
    public String getComment()
    {
        return comment;
    }
    public void setComment(String comment)
    {
        String oldValue = this.comment;
        this.comment = comment;
        firePropertyChange("comment", oldValue, comment);
    }

    @Override
    public String toString()
    {
        return getFormula();
    }
}

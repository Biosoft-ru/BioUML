package biouml.model.dynamics;

import com.developmentontheedge.beans.PropertyChangeObservable;

/**
 * Special interface that can be implemented by diagram element kernel to allow
 * kernel to be formula provider, that is corresponding rule will use formula
 * provided by kernel.
 */
public interface FormulaDelegate extends PropertyChangeObservable
{
    public String getFormula();
    
    public void setFormula(String formula);
}

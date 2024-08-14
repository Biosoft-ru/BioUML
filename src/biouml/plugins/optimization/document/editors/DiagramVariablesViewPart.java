package biouml.plugins.optimization.document.editors;

import java.util.Iterator;

import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;

public class DiagramVariablesViewPart extends DiagramParametersViewPart
{

    @Override
    protected Iterator<? extends Variable> getParametersIterator(EModel emodel)
    {
        return emodel.getVariableRoles().iterator();
    }
}



package biouml.plugins.test.editors;

import biouml.model.Diagram;
import biouml.model.Role;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.util.EModelHelper;
import biouml.plugins.test.tests.TestVariable;

import com.developmentontheedge.beans.editors.StringTagEditor;

public class TestVariableNameEditor extends StringTagEditor
{
    @Override
    public String[] getTags()
    {
        TestVariable var = (TestVariable)getBean();
        Diagram d = var.getSubDiagram();
        Role role = d.getRole();
        if( role == null || ! ( role instanceof EModel ) )
            return new String[] {};
        
        return EModelHelper.getParameters((EModel)role);
    }
}

package biouml.plugins.state.analyses;

import java.util.ArrayList;
import java.util.List;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;

import com.developmentontheedge.beans.editors.StringTagEditor;

public class DiagramElementIdSelector extends StringTagEditor
{
    @Override
    public String[] getTags()
    {
        StateChange bean = (StateChange)getBean();
        Diagram diagram = bean.getDiagram();
        if( diagram == null )
            return null;
        List<String> ids = new ArrayList<>();
        ids.add( "" );
        fillCompartmentNames( diagram, "", ids );
        return ids.toArray( new String[ids.size()] );
    }

    private void fillCompartmentNames(Compartment compartment, String prefix, List<String> result)
    {
        for( DiagramElement de : compartment )
        {
            result.add( prefix + de.getName() );
            if( de instanceof Compartment )
                fillCompartmentNames( (Compartment)de, prefix + de.getName() + ".", result );
        }
    }
}

package biouml.model.web;

import biouml.model.Diagram;
import biouml.standard.state.State;
import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;

public class StatesBeanProvider implements BeanProvider
{

    public Object getBean(String path)
    {
        DataElementPath fullPath = DataElementPath.create(path);
        String stateName = fullPath.getName();
        Diagram diagram = WebDiagramsProvider.getDiagram(fullPath.getParentPath().toString(), false);
        if ( diagram == null )
            return null;
        State state = diagram.getState(stateName);
        if ( state != null )
            return state;
        else
            return null;
    }

}

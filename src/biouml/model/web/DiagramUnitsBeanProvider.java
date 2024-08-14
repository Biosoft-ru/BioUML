package biouml.model.web;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.standard.type.Unit;
import biouml.workbench.diagram.viewpart.UnitsEditor.Units;
import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;

public class DiagramUnitsBeanProvider implements BeanProvider
{
    public Object getBean(String path)
    {
        Diagram diagram = WebDiagramsProvider.getDiagram( path, true );
        if( diagram == null )
        {
            DataElementPath fullPath = DataElementPath.create( path );
            String unitName = fullPath.getName();
            diagram = WebDiagramsProvider.getDiagram( fullPath.getParentPath().toString(), true );
            if( diagram == null )
                return null;
            EModel executableModel = diagram.getRole( EModel.class );
            Unit unit = executableModel.getUnits().get( unitName );
            if( unit != null )
                return unit;
            else
                return null;
        }
        else
        {
            EModel executableModel = diagram.getRole( EModel.class );
            return new Units( executableModel );
        }
    }
}

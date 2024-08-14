package biouml.plugins.sbml;

import javax.annotation.Nonnull;

import biouml.model.Diagram;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Base;
import ru.biosoft.access.core.DataCollection;

public class SbmlDiagramType_L1 extends SbmlDiagramType
{
    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {
        Diagram diagram = super.createDiagram(origin, diagramName, kernel);
        diagram.setRole( new SbmlEModelOld( diagram ) );
        PathwaySimulationDiagramType.DiagramPropertyChangeListener listener = new PathwaySimulationDiagramType.DiagramPropertyChangeListener(diagram);
        diagram.getViewOptions().addPropertyChangeListener(listener);
        return diagram;
    }
}
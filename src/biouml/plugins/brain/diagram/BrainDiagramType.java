package biouml.plugins.brain.diagram;

import javax.annotation.Nonnull;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramViewBuilder;
import biouml.model.SemanticController;
import biouml.standard.diagram.MathDiagramType;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import ru.biosoft.access.core.DataCollection;

@PropertyName ("Brain model")
@PropertyDescription ("Model of the brain at regional or cellular levels.")
public class BrainDiagramType extends MathDiagramType
{
    @Override
    public @Nonnull Diagram createDiagram(DataCollection<?> origin, String diagramName, Base kernel) throws Exception
    {	
        DiagramInfo info;
        if( kernel instanceof DiagramInfo )
        {
            info = (DiagramInfo)kernel;
        }
        else
        {
            info = new DiagramInfo(origin, diagramName);
        }
         
        Diagram diagram = new Diagram(origin, info, this);
        diagram.setRole(new BrainEModel(diagram));

        PathwaySimulationDiagramType.DiagramPropertyChangeListener listener = new PathwaySimulationDiagramType.DiagramPropertyChangeListener(
                diagram);
        diagram.getViewOptions().addPropertyChangeListener(listener);
         
        diagram.getViewOptions().setDependencyEdges(false);
         
        return diagram;
    }
	
    @Override
    public Object[] getNodeTypes()
    {
    	Object[] mathNodeTypes = super.getNodeTypes();
    	Object[] brainNodeTypes = {BrainType.TYPE_CONNECTIVITY_MATRIX, BrainType.TYPE_DELAY_MATRIX, 
    			BrainRegionalModel.class, BrainCellularModel.class, BrainReceptorModel.class};
    	return ArrayUtils.addAll(mathNodeTypes, brainNodeTypes);
    }
	
    @Override
    public DiagramViewBuilder getDiagramViewBuilder()
    {
        if (diagramViewBuilder == null)
        {
            diagramViewBuilder = new BrainDiagramViewBuilder();
        }

        return diagramViewBuilder;
    }
    
    @Override
    public SemanticController getSemanticController()
    {
        if (semanticController == null)
        {
            semanticController = new BrainDiagramSemanticController();
        }

        return semanticController;
    }
}

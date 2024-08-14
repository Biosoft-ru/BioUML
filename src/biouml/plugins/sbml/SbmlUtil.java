package biouml.plugins.sbml;

import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.ModelDefinition;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;

import com.developmentontheedge.beans.DynamicProperty;

public class SbmlUtil
{
    public static boolean isInternal(Diagram diagram, Diagram compositeDiagram)
    {
        DynamicProperty dp = diagram.getAttributes().getProperty(ModelDefinition.REF_MODEL_DEFINITION);
        if(dp == null)
            return false;
        ModelDefinition modelDef = (ModelDefinition)dp.getValue();
        return Diagram.getDiagram(modelDef).equals(compositeDiagram);
    }
    
    public static void inlineModelDefinitions(Diagram diagram) throws Exception
    {
        SemanticController controller = diagram.getType().getSemanticController();
        for( Diagram d : diagram.recursiveStream().select(SubDiagram.class).map(s -> s.getDiagram())
                .filter(d -> !SbmlUtil.isInternal(d, diagram)) )
        {
            diagram.put(controller.validate(diagram,
                    new ModelDefinition(diagram, d, DefaultSemanticController.generateUniqueNodeName(diagram, d.getName() + "_definition"))));
        }
    }
}

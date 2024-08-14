package biouml.plugins.brain.diagram;

import biouml.model.DiagramElement;
import biouml.model.Role;
import biouml.plugins.brain.sde.SdeEModel;
import ru.biosoft.math.model.PredefinedFunction;

@SuppressWarnings ("serial")
public class BrainEModel extends SdeEModel
{
    public static final String BRAIN_EMODEL_TYPE_STRING = "Brain EModel";
	
    public BrainEModel(DiagramElement diagramElement)
    {
        super(diagramElement);
        declareFunction(new PredefinedFunction("noise", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 1)); // should be removed after sde plugin completion
        declareFunction(new PredefinedFunction("markov", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 3));
        declareFunction(new PredefinedFunction("heaviside", ru.biosoft.math.model.Function.FUNCTION_PRIORITY, 1));        
    }
    
    @Override
    public String getType()
    {
        return BRAIN_EMODEL_TYPE_STRING;
    }
    
    @Override
    public Role clone(DiagramElement de)
    {
        BrainEModel emodel = new BrainEModel(de);
        doClone(emodel);
        return emodel;
    }
}

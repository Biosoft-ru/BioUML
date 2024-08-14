package biouml.plugins.agentmodeling;

import biouml.model.DiagramElement;
import biouml.model.Role;
import biouml.model.dynamics.EModel;

@SuppressWarnings ( "serial" )
public class AgentEModel extends EModel
{
    public AgentEModel(DiagramElement diagramElement)
    {
        super(diagramElement);
    }

    public static final String AGENT_EMODEL_TYPE = "Agent EModel";

    @Override
    public String getType()
    {
        return AGENT_EMODEL_TYPE;
    }
    
    @Override
    public Role clone(DiagramElement de)
    {
        AgentEModel emodel = new AgentEModel(de);
        doClone(emodel);
        return emodel;
    }

}

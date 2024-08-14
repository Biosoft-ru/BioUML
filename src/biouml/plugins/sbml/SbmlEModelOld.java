package biouml.plugins.sbml;

import biouml.model.DiagramElement;
import biouml.model.Role;

public class SbmlEModelOld extends SbmlEModel
{    
    public SbmlEModelOld(DiagramElement diagramElement)
    {
        super(diagramElement);
        constantsMap.remove("avogadro");
    }
    
    @Override
    public Role clone(DiagramElement de)
    {
        SbmlEModelOld emodel = new SbmlEModelOld(de);
        doClone(emodel);
        return emodel;
    }
}

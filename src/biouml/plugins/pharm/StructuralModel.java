package biouml.plugins.pharm;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramContainer;
import biouml.standard.type.Stub;

/**
 * 
 * @author Ilya
 * 
 */
public class StructuralModel extends DiagramContainer
{

    public StructuralModel(DataCollection origin, Diagram diagram, String name)
    {
        super(origin, diagram, new Stub(null, name, Type.TYPE_STRUCTURAL_MODEL));
    }
    
    @Override
    public boolean isDiagramMutable()
    {
        return false;
    }

    @Override
    public @Nonnull StructuralModel clone(Compartment newParent, String newName)
    {
        if( newParent == this )
            throw new IllegalArgumentException("Can not clone compartment into itself, compartment=" + newParent);

        StructuralModel result = new StructuralModel(newParent, this.getDiagram(), newName);
        super.doClone(result);
        return result;
    }

}

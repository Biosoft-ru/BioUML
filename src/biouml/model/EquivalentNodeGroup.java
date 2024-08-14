package biouml.model;

import java.util.Iterator;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import biouml.standard.type.Base;
import biouml.standard.type.Stub;

/**
 * Group of equivalent nodes. In view mode they are represented by one node image,
 * in design mode they look likes compartment.
 */
@PropertyName ( "Group of equivalent nodes" )
@PropertyDescription ( "Group of equivalent nodes is a special case of compartment to group nodes equivalent in a given context, for example "
        + "homologous genes or proteins.<br>Generally, in view mode group of equivalent nodesrepresented by one node image. "
        + " In design mode such group look likes usual compartment." )
public class EquivalentNodeGroup extends Compartment
{
    public EquivalentNodeGroup(DataCollection parent, String name)
    {
        super(parent, name, new Stub(parent, name));
    }

    /**
     * We would like to get access for kernels through the property to display it
     * in property inspector.
     */
    public Base[] getKernels()
    {
        Base[] kernels = new Base[getSize()];
        int i= 0;
        Iterator<DiagramElement> it = iterator();
        while(it.hasNext())
        {
            Node node = (Node)it.next();
            kernels[i++] = node.getKernel();
        }

        return kernels;
    }

    /**
     * @returns a node that represents all group in view mode.
     */
    public Node getRepresentative()
    {
        Iterator<DiagramElement> it = iterator();
        if(it.hasNext())
            return (Node)it.next();

        return null;
    }
    
    @Override
    public @Nonnull EquivalentNodeGroup clone(Compartment newParent, String newName) throws IllegalArgumentException
    {
        EquivalentNodeGroup nodeGroup = new EquivalentNodeGroup(newParent, newName);
        doClone(nodeGroup);
        return nodeGroup;
    }
}

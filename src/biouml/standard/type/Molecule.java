package biouml.standard.type;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

abstract public class Molecule extends Concept
{
    /**
     * Array of references (identifiers) to corresponding structure for molecule.
     * Each string corresponds to {@link ru.biosoft.access.core.DataElement} name from "structures" ru.biosoft.access.core.DataCollection.
     */
    private String[] structureReferences;
    
    protected Molecule( DataCollection origin, String name )
    {
        super(origin,name);
    }

    @PropertyName ( "Structure" )
    @PropertyDescription ( "Structure (2D or 3D) references. <br>"
            + "Each molecule can have several structures depending on conditions.<br>"
               + "CDK library is used to visualise the molecule structure.")
    public String[] getStructureReferences()
    {
        return structureReferences;
    }
    public void setStructureReferences(String[] structureReferences)
    {
        String[] oldValue = this.structureReferences;
        this.structureReferences = structureReferences;
        firePropertyChange("structureReferences", oldValue, structureReferences);
    }

}

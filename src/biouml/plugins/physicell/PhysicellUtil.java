package biouml.plugins.physicell;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Role;
import ru.biosoft.util.DPSUtils;

public class PhysicellUtil
{
    /**
     * Find node connected to edge with given role class or null if there is no one
     */
    public static <T extends Role> T findNode(Edge e, Class<T> role)
    {
        return e.nodes().map( n -> n.getRole() ).select( role ).findAny().orElse( null );
    }

    /**
     * Retrieve role from attribute
     */
    public static <T extends Role> T validateRole(DiagramElement de, Class<T> c, String name) throws Exception
    {
        DynamicProperty dp = de.getAttributes().getProperty( name );
        if( dp == null )
        {
            dp = DPSUtils.createHiddenReadOnly( name, c, c.getConstructor( DiagramElement.class ).newInstance( de ) );
            de.getAttributes().add( dp );
        }
        T role = c.cast( dp.getValue() );
        de.setRole( role );
        return role;
    }

    public static CellDefinitionProperties findCellDefinition(String name, MulticellEModel emodel)
    {
        for( CellDefinitionProperties cdp : emodel.getCellDefinitions() )
        {
            if( cdp.getName().equals( name ) )
                return cdp;
        }
        return null;
    }
}
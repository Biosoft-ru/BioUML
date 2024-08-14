package biouml.model.dynamics;

import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DerivedDataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Role;

public class VariablesDataCollection extends DerivedDataCollection<Variable, Variable>
{
    public VariablesDataCollection(DataCollection<?> parent, Compartment compartment) throws Exception
    {
        super(parent, compartment.getName() + " variables", new VectorDataCollection<Variable>(compartment.getName()
                + " variables(vectorDC)", null, getDataElementProperties()), null);

        // disable cache
        this.v_cache = null;

        fillCollection(compartment);
    }
    
    private static Properties getDataElementProperties()
    {
        Properties props = new Properties();
        props.setProperty(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, Variable.class.getName());
        props.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "Variables");
        return props;
    }

    protected Properties createCollectionProperties(Diagram diagram)
    {
        Properties props = new Properties();
        props.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, diagram.getName());
        return props;
    }

    /**
     * Creates some properties required by {@link DerivedDataCollection}.
     * @todo implementation
     */
    protected static Properties createProperties(Diagram diagram)
    {
        if( diagram == null )
            throw new IllegalArgumentException("'diagram' parameter can't be null.");
        Properties props = new Properties();
        props.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, diagram.getName());
        return props;
    }

    final private void fillCollection(Compartment compartment) throws Exception
    {
        for(DiagramElement element: compartment)
        {
            //Elements of subdiagrams are not included into composite diagram
            if( (element instanceof Compartment) && ! ( element instanceof Diagram ) )
            {
                Compartment subTree = (Compartment)element;
                fillCollection(subTree);
            }
            else
            {
                Role role = element.getRole();
                if( role instanceof Variable )
                    put((Variable)role);
            }
        }
    }
    
}

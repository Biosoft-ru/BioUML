
package biouml.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.AbstractDataCollection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.xml.XmlDiagramType;

/**
 * @author anna
 *
 */
public abstract class DiagramTypeConverterSupport implements DiagramTypeConverter
{
    protected static final Logger log = Logger.getLogger(DiagramTypeConverterSupport.class.getName());

    @Override
    public Diagram convert(Diagram diagram, Object type) throws Exception
    {
        DiagramType diagramType = null;
        if( type instanceof Class )
        {
            if( DiagramType.class.isAssignableFrom((Class<?>)type) )
            {
                diagramType = (DiagramType) ( (Class<?>)type ).newInstance();
            }
            else
            {
                log.log(Level.SEVERE, "Incorrect diagram type class: " + ( (Class<?>)type ).getName());
            }
        }
        else if( type instanceof String )
        {
            XmlDiagramType diagramTypeObj = XmlDiagramType.getTypeObject((String)type);
            if( diagramTypeObj != null )
            {
                diagramType = diagramTypeObj;

                //remove from cache to get an unique copy of notation
                ( (AbstractDataCollection<?>)diagramTypeObj.getOrigin() ).removeFromCache(diagramTypeObj.getName());
            }
            else
            {
                log.log(Level.SEVERE, "Incorrect graphic notation: " + (String)type);
            }
        }
        return convert(diagramType, diagram);
    }

    /**
     * Convert diagram to specific {@link DiagramType}
     */
    abstract protected Diagram convert(DiagramType diagramType, Diagram diagram) throws Exception;

    @Override
    public DiagramElement[] convertDiagramElement(DiagramElement de, Diagram diagram) throws Exception
    {
        return new DiagramElement[] {de};
    }

    @Override
    public boolean canConvert(DiagramElement de)
    {
        return true;
    }

    public void updateDiagramModel(Compartment compartment, Diagram diagram)
    {
        EModel model = diagram.getRole(EModel.class);
        for(DiagramElement de : compartment.recursiveStream())
        {
            Role deRole = de.getRole();
            if( deRole instanceof Variable )
            {
                model.put((Variable)deRole);
            }
            else if (deRole instanceof Equation)
            {
               String varName = ((Equation)deRole).getVariable();
               if ((!varName.startsWith( "$" ) || varName.startsWith( "$$" )) && !model.containsVariable( varName ))
                   model.put(new Variable(varName, model, model.getVariables() ));
            }
        }
    }

}

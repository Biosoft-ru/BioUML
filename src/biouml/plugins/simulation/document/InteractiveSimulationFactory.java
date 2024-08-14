package biouml.plugins.simulation.document;

import com.developmentontheedge.application.ApplicationDocument;
import com.developmentontheedge.application.DocumentFactory;

import biouml.model.Diagram;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;

/**
 * @author axec
 *
 */
public class InteractiveSimulationFactory implements DocumentFactory
{
    @Override
    public ApplicationDocument createDocument()
    {
        return null;
    }

    @Override
    public ApplicationDocument openDocument(String name)
    {
        try
        {
            ApplicationDocument document = null;
            DataElement de = CollectionFactory.getDataElement( name );
            if( de instanceof Diagram )
            {
                InteractiveSimulation simulation = new InteractiveSimulation( null, de.getName() + " Simulation", (Diagram)de );
                document = new InteractiveSimulationDocument( simulation );
                return document;
            }
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
        return null;
    }
}

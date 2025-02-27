package biouml.plugins.physicell.document;

import com.developmentontheedge.application.ApplicationDocument;
import com.developmentontheedge.application.DocumentFactory;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.generic.GenericDataCollection;

/**
 * @author axec
 *
 */
public class PhysicellResultFactory implements DocumentFactory
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
            if( de instanceof GenericDataCollection )
            {
                PhysicellSimulationResult simulation = new PhysicellSimulationResult( de.getName() + " Simulation", (GenericDataCollection)de );
                document = new PhysicellResultDocument( simulation );
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

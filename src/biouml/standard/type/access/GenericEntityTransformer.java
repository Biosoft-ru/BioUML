package biouml.standard.type.access;

import java.lang.reflect.Constructor;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.Entry;
import ru.biosoft.access.support.BeanInfoEntryTransformer;

/**
 * This class was re-added to this project only for supporting
 * of some old tests like biouml.model._test.TestDiagramToXML
 */
public class GenericEntityTransformer extends BeanInfoEntryTransformer
{
    public GenericEntityTransformer()
    {}

    @Override
    synchronized public DataElement transformInput(Entry input) throws Exception
    {
        Class<? extends DataElement> clazz = getOutputType().asSubclass(DataElement.class);

        if( clazz.isInstance(input) )
            return input;

        Constructor<? extends DataElement> constructor = null;
        try
        {
            constructor = clazz.getConstructor( ru.biosoft.access.core.DataCollection.class, String.class );
        }
        catch(NoSuchMethodException e)
        {
            constructor = clazz.getConstructor();
        }

        DataElement de = constructor.newInstance( getTransformedCollection(), input.getName() );

        readObject(de, input.getReader());

        return de;
    }
}

package biouml.workbench;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.repository.AbstractElementAction;
import ru.biosoft.gui.DocumentManager;

import com.developmentontheedge.application.DocumentFactory;

/**
 * New-style action which can open any document
 * @author lan
 */
@SuppressWarnings ( "serial" )
public class OpenDocumentAction extends AbstractElementAction
{
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        DocumentManager.getDocumentManager().openDocument(de);
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        if(de instanceof DataCollection && !((DataCollection<?>)de).isValid())
            return false;
        if( de.getOrigin() == null )
            return false;
        DataElementDescriptor descriptor = de.getOrigin().getDescriptor(de.getName());
        if(descriptor == null)
            return false;
        DocumentFactory documentFactory = DocumentManager.getDocumentManager().getDocumentFactory(descriptor.getType());
        return documentFactory != null;
    }
}

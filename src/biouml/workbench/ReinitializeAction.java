package biouml.workbench;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.repository.AbstractElementAction;
import ru.biosoft.access.security.CredentialsCollection;

/**
 * @author lan
 */
@SuppressWarnings ( "serial" )
public class ReinitializeAction extends AbstractElementAction
{
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        ((DataCollection<?>)de).reinitialize();
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return ( de instanceof DataCollection ) && ! ( (DataCollection<?>)de ).isValid()
                && ( ! ( de instanceof CredentialsCollection ) || !( (CredentialsCollection)de ).needCredentials() );
    }
}

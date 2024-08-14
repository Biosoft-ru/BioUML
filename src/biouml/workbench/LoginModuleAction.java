package biouml.workbench;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.exception.CollectionLoginException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.repository.AbstractElementAction;
import ru.biosoft.access.security.CredentialsCollection;
import ru.biosoft.util.PropertiesDialog;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

@SuppressWarnings ( "serial" )
public class LoginModuleAction extends AbstractElementAction
{
    @Override
    protected void performAction(DataElement de) throws Exception
    {
        CredentialsCollection dc = (CredentialsCollection)de;
        Object credentialsBean = dc.getCredentialsBean();
        PropertiesDialog dialog = new PropertiesDialog( Application.getApplicationFrame(), "Login", credentialsBean );
        boolean complete = false;
        while( !complete && dialog.doModal() )
        {
            try
            {
                dc.processCredentialsBean( credentialsBean );
                complete = true;
            }
            catch( CollectionLoginException ex )
            {
                ApplicationUtils.errorBox( "Login failed", ExceptionRegistry.log( ex ) );
            }
        }
    }

    @Override
    protected boolean isApplicable(DataElement de)
    {
        return de instanceof CredentialsCollection &&
                ((CredentialsCollection)de).needCredentials();
    }
}

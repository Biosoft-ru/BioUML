package biouml.plugins.jupyter.auth;

import java.util.ArrayList;
import java.util.List;

import biouml.plugins.jupyter.configuration.JupyterConfiguration;

public class SingleUserJupyterAccessor extends BioumlJupyterAccessor
{
    protected final String user;
    protected final String password;

    public SingleUserJupyterAccessor(JupyterConfiguration configuration)
    {
        super( configuration );
        user = configuration.getSingleUserName();
        password = configuration.getSingleUserPassword();
    }

    @Override
    public List<String> getAuthCookies(String user, String password)
    {
        //ignores given parameters, use inner configuration
        return getAuthCookies();
    }

    private List<String> getAuthCookies()
    {
        if( this.user != null && this.password != null )
            return super.getAuthCookies( user, password );
        return new ArrayList<>();
    }

}

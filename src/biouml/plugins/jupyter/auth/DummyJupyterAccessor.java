package biouml.plugins.jupyter.auth;

import java.util.ArrayList;
import java.util.List;

import biouml.plugins.jupyter.configuration.JupyterConfiguration;

/**
 * Do not try to login user automatically. Always return empty list
 * 
 * @author manikitos
 */
public class DummyJupyterAccessor implements JupyterAccessor
{
    public DummyJupyterAccessor()
    {
    }

    public DummyJupyterAccessor(@SuppressWarnings ( "unused" ) JupyterConfiguration configuration)
    {
        //ignores configuration properties, since do nothing
        this();
    }

    @Override
    public List<String> getAuthCookies(String user, String password)
    {
        return new ArrayList<>();
    }

}
